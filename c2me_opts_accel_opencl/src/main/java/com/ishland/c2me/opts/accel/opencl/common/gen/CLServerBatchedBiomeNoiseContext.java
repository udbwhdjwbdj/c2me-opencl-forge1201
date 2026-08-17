package com.ishland.c2me.opts.accel.opencl.common.gen;

import com.ishland.c2me.base.common.GlobalExecutors;
import com.ishland.c2me.base.common.scheduler.IVanillaChunkManager;
import com.ishland.c2me.base.mixin.access.IChunkSection;
import com.ishland.c2me.opts.accel.opencl.common.Config;
import com.ishland.c2me.opts.accel.opencl.common.compiler.GeneratedCLSource;
import com.ishland.c2me.opts.accel.opencl.common.compiler.OpenCLCGen;
import com.ishland.c2me.opts.accel.opencl.common.compiler.emitters.misc.CLBlockStateMappings;
import com.ishland.c2me.opts.accel.opencl.common.ducks.PalettedContainerExtension;
import com.ishland.c2me.opts.accel.opencl.common.gen.cache.CLBufferCache;
import com.ishland.c2me.opts.accel.opencl.common.gen.cache.Stage1Cache;
import com.ishland.c2me.opts.accel.opencl.common.util.CLUtil;
import com.ishland.c2me.opts.accel.opencl.common.util.TLUtil;
import com.ishland.c2me.opts.accel.opencl.common.workarounds.Workarounds;
import com.ishland.c2me.opts.accel.opencl.common.util.FlowschedAssertions;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import com.ishland.c2me.opts.accel.opencl.common.util.StaticCache2D;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CL12;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CLServerBatchedBiomeNoiseContext {
   private static final Logger LOGGER = LoggerFactory.getLogger(CLServerBatchedBiomeNoiseContext.class);
   private static final BlockState AIR = Blocks.AIR.defaultBlockState();
   public static final int BATCH_SIZE = Config.useSmallerBatches ? 2 : 4;
   public static final int BATCH_MASK = BATCH_SIZE - 1;
   public static final int BATCH_SHIFT = Integer.bitCount(BATCH_MASK);
   private final ChunkPos startingPos;
   private final CLServerWorldContext worldContext;
   private final NoiseBasedChunkGenerator generator;
   private final RandomState noiseConfig;

   public static boolean isAligned(int x, int z) {
      return (x & BATCH_MASK) == 0 && (z & BATCH_MASK) == 0;
   }

   public CLServerBatchedBiomeNoiseContext(ChunkPos startingPos, CLServerWorldContext worldContext, NoiseBasedChunkGenerator generator, RandomState noiseConfig) {
      this.startingPos = Objects.requireNonNull(startingPos);
      this.worldContext = Objects.requireNonNull(worldContext);
      this.generator = Objects.requireNonNull(generator);
      this.noiseConfig = Objects.requireNonNull(noiseConfig);
   }

   public CompletableFuture<Void> execute(IVanillaChunkManager chunkManager, StaticCache2D<ProtoChunk> chunks, StaticCache2D<StructureManager> structureAccessors) {
      return this.worldContext
         .getEstimateSurfaceHeightCache()
         .getAreaCache(this.startingPos.x, this.startingPos.z, BATCH_SIZE, BATCH_SIZE)
         .thenComposeAsync(
            cacheEntry -> {
               if (TLUtil.stage1CachePassing.get() != null) {
                  throw new IllegalStateException("Reentrance");
               }

               TLUtil.stage1CachePassing.set(cacheEntry);
               ByteBuffer rwData;
               try {
                  rwData = CLDataUtil.worldgen_data_root$createForArea(
                     this.startingPos,
                     BATCH_SIZE,
                     chunks,
                     this.generator,
                     this.noiseConfig,
                     structureAccessors,
                     this.worldContext.getClBlockStateMappings(),
                     this.worldContext.getGeneratedCLSource(),
                     cacheEntry
                  );
               } finally {
                  TLUtil.stage1CachePassing.remove();
               }
               return this.worldContext
                  .borrowCommandQueue()
                  .thenCompose(
                     pair -> CompletableFuture.<CompletableFuture<Void>>supplyAsync(
                              () -> this.execute0(
                                    chunks,
                                    structureAccessors,
                                    this.worldContext.getClBlockStateMappings(),
                                    rwData,
                                    cacheEntry,
                                    (OpenCLDevice.BorrowedCommandQueue)pair.left(),
                                    (CLServerWorldContext.DeviceWithProgram)pair.right()
                                 ),
                              ((OpenCLDevice.BorrowedCommandQueue)pair.left()).getDevice().getExecutor()
                           )
                           .thenCompose(Function.identity())
                  );
            },
            chunkManager.c2me$getSchedulingManager().getExecutor()
         );
   }

   private CompletableFuture<Void> execute0(
      StaticCache2D<ProtoChunk> chunks,
      StaticCache2D<StructureManager> structureAccessors,
      CLBlockStateMappings clBlockStateMappings,
      ByteBuffer rwData,
      Stage1Cache.AreaCacheEntry cacheEntry,
      OpenCLDevice.BorrowedCommandQueue commandQueue,
      CLServerWorldContext.DeviceWithProgram deviceWithProgram
   ) {
      CompletableFuture<Void> future = new CompletableFuture<>();

      try {
         MemoryStack stack = MemoryStack.stackPush();

         try {
            GeneratedCLSource generatedCLSource = this.worldContext.getGeneratedCLSource();
            IntBuffer errorCodeRet = stack.callocInt(1);
            NoiseGeneratorSettings settings = (NoiseGeneratorSettings)this.generator.generatorSettings().value();
            int verticalCellBlockCount = settings.noiseSettings().getCellHeight();
            FlowschedAssertions.assertTrue(Math.floorDiv(16, settings.noiseSettings().getCellWidth()) * settings.noiseSettings().getCellWidth() == 16);
            int horizontalCellsCount = Math.floorDiv(16, settings.noiseSettings().getCellWidth()) * BATCH_SIZE;
            int verticalCellsCount = Math.floorDiv(settings.noiseSettings().height(), verticalCellBlockCount);
            int horizontalSize = 16 * BATCH_SIZE;
            int verticalSize = verticalCellsCount * verticalCellBlockCount;
            ProtoChunk startingChunk = (ProtoChunk)chunks.get(this.startingPos.x, this.startingPos.z);
            LevelHeightAccessor heightLimitView = startingChunk.getHeightAccessorForGeneration();
            int biomeHeight = (heightLimitView.getMaxSection() - heightLimitView.getMinSection()) * 4;
            if (TLUtil.stage1CachePassing.get() != null) {
               throw new IllegalStateException("Reentrance");
            }

            CLBufferCache.BufferEntry rwBuffer = deviceWithProgram.device()
               .getBufferCache()
               .allocate(
                  CLBufferCache.Type.GEN_BATCHING_RW_DATA, rwData.remaining(), size -> CL12.clCreateBuffer(commandQueue.getContext(), 1L, size, errorCodeRet)
               );
            CLUtil.checkCLError(errorCodeRet);
            int biomeOutCount = biomeHeight * 4 * BATCH_SIZE * 4 * BATCH_SIZE;
            CLBufferCache.BufferEntry biomeOutBuffer = deviceWithProgram.device()
               .getBufferCache()
               .allocate(
                  CLBufferCache.Type.GEN_BATCHING_BIOME_RX, biomeOutCount * 4, size -> CL12.clCreateBuffer(commandQueue.getContext(), 2L, size, errorCodeRet)
               );
            CLUtil.checkCLError(errorCodeRet);
            long blockOutBufferSize = (long)horizontalSize * (long)verticalSize * (long)horizontalSize * 4L;
            CLBufferCache.BufferEntry blockOutBuffer = deviceWithProgram.device()
               .getBufferCache()
               .allocate(
                  CLBufferCache.Type.GEN_BATCHING_BLOCK_STATE_RX,
                  Math.toIntExact(blockOutBufferSize),
                  size -> CL12.clCreateBuffer(commandQueue.getContext(), 2L, size, errorCodeRet)
               );
            CLUtil.checkCLError(errorCodeRet);
            IntBuffer biomeOutBufferData = MemoryUtil.memAllocInt(biomeOutCount);
            ByteBuffer blockOutBufferData = MemoryUtil.memAlloc(horizontalSize * verticalSize * horizontalSize);
            LongList eventsToRelease = new LongArrayList();
            LongList kernelsToRelease = new LongArrayList();
            PointerBuffer event = stack.callocPointer(1);
            CLUtil.checkCLError(CL12.clEnqueueWriteBuffer(commandQueue.getCommandQueue(), rwBuffer.buffer(), false, 0L, rwData, null, event));
            eventsToRelease.add(event.get(0));
            long rwBufferWriteEvent0 = event.get(0);
            long biomeOutBufferReadEvent0;
            if (generatedCLSource.getBiomeMappings() != null) {
               MemoryStack var35 = MemoryStack.stackPush();

               try {
                  PointerBuffer workSize = stack.mallocPointer(3);
                  workSize.put(0, (long)(4 * BATCH_SIZE));
                  workSize.put(1, (long)(4 * BATCH_SIZE));
                  workSize.put(2, (long)biomeHeight);
                  workSize.rewind();
                  PointerBuffer local = stack.callocPointer(3);
                  local.put(0, 8L);
                  local.put(1, 8L);
                  local.put(2, 1L);
                  local.rewind();
                  PointerBuffer eventWaitList = stack.callocPointer(1);
                  eventWaitList.put(0, rwBufferWriteEvent0);
                  eventWaitList.rewind();
                  long kernel = CL12.clCreateKernel(
                     deviceWithProgram.getProgram(OpenCLCGen.ProgramType.BIOME_MULTINOISE_KERNEL), "df_biome_multinoise_kernel", errorCodeRet
                  );
                  CLUtil.checkCLError(errorCodeRet);
                  CLUtil.checkCLError(CL12.clSetKernelArg1p(kernel, 0, deviceWithProgram.programConstDataBuffer()));
                  CLUtil.checkCLError(CL12.clSetKernelArg1p(kernel, 1, rwBuffer.buffer()));
                  CLUtil.checkCLError(CL12.clSetKernelArg1p(kernel, 2, biomeOutBuffer.buffer()));
                  CLUtil.checkCLError(CL12.clSetKernelArg1i(kernel, 3, QuartPos.fromBlock(this.startingPos.getMinBlockX())));
                  CLUtil.checkCLError(CL12.clSetKernelArg1i(kernel, 4, QuartPos.fromBlock(this.startingPos.getMinBlockZ())));
                  CLUtil.checkCLError(CL12.clSetKernelArg1i(kernel, 5, QuartPos.fromBlock(heightLimitView.getMinBuildHeight())));
                  CLUtil.checkCLError(CL12.clSetKernelArg1i(kernel, 6, 4 * BATCH_SIZE));
                  CLUtil.checkCLError(CL12.clSetKernelArg1i(kernel, 7, 4 * BATCH_SIZE));
                  CLUtil.checkCLError(CL12.clSetKernelArg1i(kernel, 8, biomeHeight));
                  CLUtil.checkCLError(CL12.clEnqueueNDRangeKernel(commandQueue.getCommandQueue(), kernel, 3, null, workSize, local, eventWaitList, event));
                  eventsToRelease.add(event.get(0));
                  kernelsToRelease.add(kernel);
                  long kernelExecEvent0 = event.get(0);
                  eventWaitList.put(0, kernelExecEvent0);
                  eventWaitList.rewind();
                  CLUtil.checkCLError(
                     CL12.clEnqueueReadBuffer(commandQueue.getCommandQueue(), biomeOutBuffer.buffer(), false, 0L, biomeOutBufferData, eventWaitList, event)
                  );
                  eventsToRelease.add(event.get(0));
                  biomeOutBufferReadEvent0 = event.get(0);
                  event.put(0, rwBufferWriteEvent0);
               } catch (Throwable var58) {
                  if (var35 != null) {
                     try {
                        var35.close();
                     } catch (Throwable var53) {
                        var58.addSuppressed(var53);
                     }
                  }

                  throw var58;
               }

               if (var35 != null) {
                  var35.close();
               }
            } else {
               biomeOutBufferReadEvent0 = 0L;
            }

            MemoryStack var63 = MemoryStack.stackPush();

            try {
               PointerBuffer globalWorkSize = stack.callocPointer(3);
               globalWorkSize.put(0, (long)(horizontalCellsCount + 1));
               globalWorkSize.put(2, (long)(verticalCellsCount + 1));
               globalWorkSize.put(1, (long)(horizontalCellsCount + 1));
               PointerBuffer localWorkSize;
               if (deviceWithProgram.device().getMetadata().supportsNonUniformWorkgroups) {
                  localWorkSize = stack.callocPointer(3);
                  localWorkSize.put(0, 16L);
                  localWorkSize.put(1, 16L);
                  localWorkSize.put(2, 1L);
               } else {
                  localWorkSize = null;
               }

               if (generatedCLSource.getInterpolatorPrefills() != 0) {
                  PointerBuffer prevEvent = event.get(0) != 0L ? stack.mallocPointer(1).put(0, event.get(0)) : null;
                  long kernel = CL12.clCreateKernel(
                     deviceWithProgram.getProgram(OpenCLCGen.ProgramType.INTERPOLATOR_PREFILL), "df_interpolator_buffer_prefill_kernel", errorCodeRet
                  );
                  CLUtil.checkCLError(errorCodeRet);
                  CLUtil.checkCLError(CL12.clSetKernelArg1p(kernel, 0, deviceWithProgram.programConstDataBuffer()));
                  CLUtil.checkCLError(CL12.clSetKernelArg1p(kernel, 1, rwBuffer.buffer()));
                  CLUtil.checkCLError(
                     CL12.clEnqueueNDRangeKernel(commandQueue.getCommandQueue(), kernel, 3, null, globalWorkSize, localWorkSize, prevEvent, event)
                  );
                  eventsToRelease.add(event.get(0));
                  kernelsToRelease.add(kernel);
               }
            } catch (Throwable var59) {
               if (var63 != null) {
                  try {
                     var63.close();
                  } catch (Throwable var52) {
                     var59.addSuppressed(var52);
                  }
               }

               throw var59;
            }

            if (var63 != null) {
               var63.close();
            }

            if (settings.isAquifersEnabled()) {
               var63 = MemoryStack.stackPush();

               try {
                  int startX = Math.floorDiv(this.startingPos.getMinBlockX() - 5, 16) + 0;
                  int startY = Math.floorDiv(settings.noiseSettings().minY() + 1, 12) - 1;
                  int startZ = Math.floorDiv(this.startingPos.getMinBlockZ() - 5, 16) + 0;
                  ChunkPos endChunkPos = new ChunkPos(this.startingPos.x + BATCH_SIZE - 1, this.startingPos.z + BATCH_SIZE - 1);
                  int endX = Math.floorDiv(endChunkPos.getMaxBlockX() + 5 - 1, 16) + 1;
                  int endY = Math.floorDiv(settings.noiseSettings().minY() + settings.noiseSettings().height() - 1, 12) + 1;
                  int endZ = Math.floorDiv(endChunkPos.getMaxBlockZ() + 5 - 1, 16) + 1;
                  PointerBuffer globalWorkSizex = stack.callocPointer(3);
                  globalWorkSizex.put(0, (long)(endX - startX + 1));
                  globalWorkSizex.put(2, (long)(endY - startY + 1));
                  globalWorkSizex.put(1, (long)(endZ - startZ + 1));
                  PointerBuffer prevEvent = event.get(0) != 0L ? stack.mallocPointer(1).put(0, event.get(0)) : null;
                  long kernel = CL12.clCreateKernel(deviceWithProgram.getProgram(OpenCLCGen.ProgramType.AQUIFER_PREFILL), "aquifer_data_prefill", errorCodeRet);
                  CLUtil.checkCLError(errorCodeRet);
                  CLUtil.checkCLError(CL12.clSetKernelArg1p(kernel, 0, deviceWithProgram.programConstDataBuffer()));
                  CLUtil.checkCLError(CL12.clSetKernelArg1p(kernel, 1, rwBuffer.buffer()));
                  CLUtil.checkCLError(CL12.clEnqueueNDRangeKernel(commandQueue.getCommandQueue(), kernel, 3, null, globalWorkSizex, null, prevEvent, event));
                  eventsToRelease.add(event.get(0));
                  kernelsToRelease.add(kernel);
               } catch (Throwable var57) {
                  if (var63 != null) {
                     try {
                        var63.close();
                     } catch (Throwable var51) {
                        var57.addSuppressed(var51);
                     }
                  }

                  throw var57;
               }

               if (var63 != null) {
                  var63.close();
               }
            }

            if (generatedCLSource.getCache2dPrefills() != 0) {
               var63 = MemoryStack.stackPush();

               try {
                  PointerBuffer globalWorkSizex = stack.callocPointer(3);
                  globalWorkSizex.put(0, (long)horizontalSize);
                  globalWorkSizex.put(1, (long)horizontalSize);
                  globalWorkSizex.put(2, (long)generatedCLSource.getCache2dPrefills());
                  PointerBuffer localWorkSizex = stack.callocPointer(3);
                  localWorkSizex.put(0, 8L);
                  localWorkSizex.put(1, 8L);
                  localWorkSizex.put(2, 1L);
                  PointerBuffer prevEvent = event.get(0) != 0L ? stack.mallocPointer(1).put(0, event.get(0)) : null;
                  long kernel = CL12.clCreateKernel(
                     deviceWithProgram.getProgram(OpenCLCGen.ProgramType.CACHE2D_PREFILL), "df_cache2d_prefill_kernel", errorCodeRet
                  );
                  CLUtil.checkCLError(errorCodeRet);
                  CLUtil.checkCLError(CL12.clSetKernelArg1p(kernel, 0, deviceWithProgram.programConstDataBuffer()));
                  CLUtil.checkCLError(CL12.clSetKernelArg1p(kernel, 1, rwBuffer.buffer()));
                  CLUtil.checkCLError(
                     CL12.clEnqueueNDRangeKernel(commandQueue.getCommandQueue(), kernel, 3, null, globalWorkSizex, localWorkSizex, prevEvent, event)
                  );
                  eventsToRelease.add(event.get(0));
                  kernelsToRelease.add(kernel);
               } catch (Throwable var56) {
                  if (var63 != null) {
                     try {
                        var63.close();
                     } catch (Throwable var50) {
                        var56.addSuppressed(var50);
                     }
                  }

                  throw var56;
               }

               if (var63 != null) {
                  var63.close();
               }
            }

            var63 = MemoryStack.stackPush();

            try {
               PointerBuffer globalWorkSizex = stack.callocPointer(3);
               globalWorkSizex.put(0, (long)horizontalSize);
               globalWorkSizex.put(2, (long)verticalSize);
               globalWorkSizex.put(1, (long)horizontalSize);
               PointerBuffer localWorkSizex = stack.callocPointer(3);
               localWorkSizex.put(0, 16L);
               localWorkSizex.put(1, 16L);
               localWorkSizex.put(2, 1L);
               PointerBuffer prevEvent = event.get(0) != 0L ? stack.mallocPointer(1).put(0, event.get(0)) : null;
               long kernel = CL12.clCreateKernel(deviceWithProgram.getProgram(OpenCLCGen.ProgramType.NOISE_KERNEL), "df_noise_kernel", errorCodeRet);
               CLUtil.checkCLError(errorCodeRet);
               CLUtil.checkCLError(CL12.clSetKernelArg1p(kernel, 0, deviceWithProgram.programConstDataBuffer()));
               CLUtil.checkCLError(CL12.clSetKernelArg1p(kernel, 1, rwBuffer.buffer()));
               CLUtil.checkCLError(CL12.clSetKernelArg1p(kernel, 2, blockOutBuffer.buffer()));
               CLUtil.checkCLError(CL12.clSetKernelArg1i(kernel, 3, this.startingPos.x));
               CLUtil.checkCLError(CL12.clSetKernelArg1i(kernel, 4, this.startingPos.z));
               CLUtil.checkCLError(
                  CL12.clEnqueueNDRangeKernel(commandQueue.getCommandQueue(), kernel, 3, null, globalWorkSizex, localWorkSizex, prevEvent, event)
               );
               eventsToRelease.add(event.get(0));
               kernelsToRelease.add(kernel);
            } catch (Throwable var55) {
               if (var63 != null) {
                  try {
                     var63.close();
                  } catch (Throwable var49) {
                     var55.addSuppressed(var49);
                  }
               }

               throw var55;
            }

            if (var63 != null) {
               var63.close();
            }

            var63 = MemoryStack.stackPush();

            try {
               PointerBuffer eventWaitList;
               if (biomeOutBufferReadEvent0 != 0L) {
                  eventWaitList = stack.callocPointer(2);
                  FlowschedAssertions.assertTrue(event.get(0) != 0L);
                  eventWaitList.put(0, event.get(0));
                  eventWaitList.put(1, biomeOutBufferReadEvent0);
                  eventWaitList.rewind();
               } else {
                  eventWaitList = stack.callocPointer(1);
                  FlowschedAssertions.assertTrue(event.get(0) != 0L);
                  eventWaitList.put(0, event.get(0));
                  eventWaitList.rewind();
               }

               CLUtil.checkCLError(
                  CL12.clEnqueueReadBuffer(commandQueue.getCommandQueue(), blockOutBuffer.buffer(), false, 0L, blockOutBufferData, eventWaitList, event)
               );
               eventsToRelease.add(event.get(0));
            } catch (Throwable var54) {
               if (var63 != null) {
                  try {
                     var63.close();
                  } catch (Throwable var48) {
                     var54.addSuppressed(var48);
                  }
               }

               throw var54;
            }

            if (var63 != null) {
               var63.close();
            }

            if (Config.doExplicitFlushes || deviceWithProgram.device().getWorkarounds().contains(Workarounds.Reference.REQUIRE_EXPLICIT_FLUSHES)) {
               CLUtil.checkCLError(CL12.clFlush(commandQueue.getCommandQueue()));
            }

            deviceWithProgram.device()
               .getEventCallbackManager()
               .registerCallback(event.get(0), 0, (var20x, event_command_status, var23x) -> GlobalExecutors.executor.execute(() -> {
                     try {
                        try {
                           if (event_command_status != 0) {
                              LOGGER.error("OpenCL command failed: {}", event_command_status);
                              future.completeExceptionally(new RuntimeException("OpenCL command failed: " + event_command_status));
                           } else {
                              if (generatedCLSource.getBiomeMappings() != null) {
                                 this.writeBiomes(chunks, generatedCLSource, biomeHeight, biomeOutBufferData);
                              } else {
                                 this.genBiomesFallback(chunks, structureAccessors);
                              }

                              this.writeBlocks(chunks, clBlockStateMappings, verticalSize, settings, horizontalSize, blockOutBufferData);
                              future.complete(null);
                           }
                        } finally {
                           try {
                              deviceWithProgram.device().getExecutor().execute(() -> {
                                 deviceWithProgram.device().getBufferCache().returnBuffer(CLBufferCache.Type.GEN_BATCHING_RW_DATA, rwBuffer);
                                 deviceWithProgram.device().getBufferCache().returnBuffer(CLBufferCache.Type.GEN_BATCHING_BLOCK_STATE_RX, blockOutBuffer);
                                 deviceWithProgram.device().getBufferCache().returnBuffer(CLBufferCache.Type.GEN_BATCHING_BIOME_RX, biomeOutBuffer);

                                 for (int i = 0; i < eventsToRelease.size(); i++) {
                                    try {
                                       CLUtil.checkCLError(CL12.clReleaseEvent(eventsToRelease.getLong(i)));
                                    } catch (Throwable var9x) {
                                       LOGGER.error("Failed to release event", var9x);
                                    }
                                 }

                                 for (int i = 0; i < kernelsToRelease.size(); i++) {
                                    try {
                                       CLUtil.checkCLError(CL12.clReleaseKernel(kernelsToRelease.getLong(i)));
                                    } catch (Throwable var8x) {
                                       LOGGER.error("Failed to release kernel", var8x);
                                    }
                                 }
                              });
                              MemoryUtil.memFree(rwData);
                              MemoryUtil.memFree(blockOutBufferData);
                              MemoryUtil.memFree(biomeOutBufferData);
                              commandQueue.close();
                           } catch (Throwable var28x) {
                              LOGGER.error("Error cleaning up", var28x);
                           }
                        }
                     } catch (Throwable var30x) {
                        future.completeExceptionally(var30x);
                     }
                  }));
         } catch (Throwable var60) {
            if (stack != null) {
               try {
                  stack.close();
               } catch (Throwable var47) {
                  var60.addSuppressed(var47);
               }
            }

            throw var60;
         }

         if (stack != null) {
            stack.close();
         }
      } catch (Throwable var61) {
         future.completeExceptionally(var61);
      }

      future.exceptionally(throwable -> {
         LOGGER.error("CLServerBatchedBiomeNoiseContext threw exception", throwable);
         return null;
      });
      return future.thenApply(Function.identity())
         .orTimeout(120L, TimeUnit.SECONDS)
         .exceptionallyCompose(
            throwable -> throwable instanceof TimeoutException
                  ? CompletableFuture.failedFuture(
                     new TimeoutException(
                        String.format(
                           "CLServerBatchedBiomeNoiseContext timed out for batch %s on %s", this.startingPos.toString(), deviceWithProgram.device().toString()
                        )
                     )
                  )
                  : CompletableFuture.failedFuture(throwable)
         );
   }

   private void genBiomesFallback(StaticCache2D<ProtoChunk> chunks, StaticCache2D<StructureManager> structureAccessors) {
      for (int chunkOffX = 0; chunkOffX < BATCH_SIZE; chunkOffX++) {
         for (int chunkOffZ = 0; chunkOffZ < BATCH_SIZE; chunkOffZ++) {
            ProtoChunk chunk = (ProtoChunk)chunks.get(this.startingPos.x + chunkOffX, this.startingPos.z + chunkOffZ);
            if (!chunk.getStatus().isOrAfter(ChunkStatus.BIOMES)) {
               this.generator
                  .createBiomes(Runnable::run, this.noiseConfig, Blender.empty(), (StructureManager) structureAccessors.get(chunk.getPos().x, chunk.getPos().z), chunk);
               chunk.setStatus(ChunkStatus.BIOMES);
            }
         }
      }
   }

   private void writeBiomes(StaticCache2D<ProtoChunk> chunks, GeneratedCLSource generatedCLSource, int biomeHeight, IntBuffer biomeOutBufferData) {
      Holder<Biome>[] biomeMappings = generatedCLSource.getBiomeMappings();
      ProtoChunk startingChunk = (ProtoChunk)chunks.get(this.startingPos.x, this.startingPos.z);
      int curSectionIndex = startingChunk.getSectionIndex(startingChunk.getMinBuildHeight());
      LevelChunkSection[] chunkSection = new LevelChunkSection[BATCH_SIZE * BATCH_SIZE];
      PalettedContainer<Holder<Biome>>[] sectionBiome = new PalettedContainer[BATCH_SIZE * BATCH_SIZE];
      BitSet writeProtectedMask = new BitSet(BATCH_SIZE * BATCH_SIZE);

      for (int chunkOffZ = 0; chunkOffZ < BATCH_SIZE; chunkOffZ++) {
         for (int chunkOffX = 0; chunkOffX < BATCH_SIZE; chunkOffX++) {
            int idx = (chunkOffZ << BATCH_SHIFT) + chunkOffX;
            ProtoChunk chunk = (ProtoChunk)chunks.get(this.startingPos.x + chunkOffX, this.startingPos.z + chunkOffZ);
            boolean writeProtected = chunk.getStatus().isOrAfter(ChunkStatus.BIOMES);
            if (writeProtected) {
               writeProtectedMask.set(idx);
            } else {
               chunkSection[idx] = chunk.getSection(curSectionIndex);
               sectionBiome[idx] = chunkSection[idx].getBiomes().recreate();
            }
         }
      }

      for (int y = 0; y < biomeHeight; y++) {
         int biomeY = QuartPos.fromBlock(startingChunk.getMinBuildHeight()) + y;
         int sectionIndex = startingChunk.getSectionIndex(QuartPos.toBlock(biomeY));
         if (sectionIndex != curSectionIndex) {
            for (int chunkOffZ = 0; chunkOffZ < BATCH_SIZE; chunkOffZ++) {
               for (int chunkOffXx = 0; chunkOffXx < BATCH_SIZE; chunkOffXx++) {
                  int idx = (chunkOffZ << BATCH_SHIFT) + chunkOffXx;
                  if (!writeProtectedMask.get(idx)) {
                     setBiomeContainerReflect(chunkSection[idx], sectionBiome[idx]);
                     chunkSection[idx] = ((ProtoChunk)chunks.get(this.startingPos.x + chunkOffXx, this.startingPos.z + chunkOffZ)).getSection(sectionIndex);
                     sectionBiome[idx] = chunkSection[idx].getBiomes().recreate();
                  }
               }
            }

            curSectionIndex = sectionIndex;
         }

         int horizontalBiomeSize = 4 << BATCH_SHIFT;

         for (int z = 0; z < horizontalBiomeSize; z++) {
            for (int x = 0; x < horizontalBiomeSize; x++) {
               int value = biomeOutBufferData.get((y * horizontalBiomeSize + z) * horizontalBiomeSize + x);
               int chunkOffXxx = x >> 2 & BATCH_MASK;
               int chunkOffZ = z >> 2 & BATCH_MASK;
               int idx = (chunkOffZ << BATCH_SHIFT) + chunkOffXxx;
               if (!writeProtectedMask.get(idx)) {
                  ((PalettedContainerExtension)sectionBiome[idx]).c2me$setUnsafe(x & 3, biomeY & 3, z & 3, biomeMappings[value]);
               }
            }
         }
      }

      for (int chunkOffXxx = 0; chunkOffXxx < BATCH_SIZE; chunkOffXxx++) {
         for (int chunkOffZ = 0; chunkOffZ < BATCH_SIZE; chunkOffZ++) {
            int idx = (chunkOffZ << BATCH_SHIFT) + chunkOffXxx;
            if (!writeProtectedMask.get(idx)) {
               setBiomeContainerReflect(chunkSection[idx], sectionBiome[idx]);
               ProtoChunk chunk = (ProtoChunk)chunks.get(this.startingPos.x + chunkOffXxx, this.startingPos.z + chunkOffZ);
               chunk.setStatus(ChunkStatus.BIOMES);
            }
         }
      }
   }

   private static void setBiomeContainerReflect(Object section, Object biomeContainer) {
      ((com.ishland.c2me.opts.accel.opencl.mixin.access.ILevelChunkSectionAccess) section).c2me$setBiomes((net.minecraft.world.level.chunk.PalettedContainerRO<net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome>>) biomeContainer);
   }

   private void writeBlocks(
      StaticCache2D<ProtoChunk> chunks,
      CLBlockStateMappings clBlockStateMappings,
      int verticalSize,
      NoiseGeneratorSettings settings,
      int horizontalSize,
      ByteBuffer blockOutBufferData
   ) {
      {
         ProtoChunk startingChunk = (ProtoChunk)chunks.get(this.startingPos.x, this.startingPos.z);
         int curSectionIndex = startingChunk.getSectionIndex(startingChunk.getMinBuildHeight());
         LevelChunkSection[] chunkSection = new LevelChunkSection[BATCH_SIZE * BATCH_SIZE];
         int writeProtectedMask = 0;
         MutableBlockPos mutablePos = new MutableBlockPos();

         for (int chunkOffZ = 0; chunkOffZ < BATCH_SIZE; chunkOffZ++) {
            for (int chunkOffX = 0; chunkOffX < BATCH_SIZE; chunkOffX++) {
               int idx = (chunkOffZ << BATCH_SHIFT) + chunkOffX;
               ProtoChunk chunk = (ProtoChunk)chunks.get(this.startingPos.x + chunkOffX, this.startingPos.z + chunkOffZ);
               boolean writeProtected = chunk.getStatus().isOrAfter(ChunkStatus.NOISE);
               writeProtectedMask |= writeProtected ? 1 << idx : 0;
               if (!writeProtected) {
                  chunkSection[idx] = chunk.getSection(curSectionIndex);
                  chunkSection[idx].acquire();
               }
            }
         }

         for (int y = 0; y < verticalSize; y++) {
            int blockY = settings.noiseSettings().minY() + y;
            int sectionIndex = startingChunk.getSectionIndex(blockY);
            if (sectionIndex != curSectionIndex) {
               for (int chunkOffZ = 0; chunkOffZ < BATCH_SIZE; chunkOffZ++) {
                  for (int chunkOffXx = 0; chunkOffXx < BATCH_SIZE; chunkOffXx++) {
                     int idx = (chunkOffZ << BATCH_SHIFT) + chunkOffXx;
                     if (((long)writeProtectedMask & 1L << idx) == 0L) {
                        chunkSection[idx].recalcBlockCounts();
                        chunkSection[idx].release();
                        chunkSection[idx] = ((ProtoChunk)chunks.get(this.startingPos.x + chunkOffXx, this.startingPos.z + chunkOffZ)).getSection(sectionIndex);
                        chunkSection[idx].acquire();
                     }
                  }
               }

               curSectionIndex = sectionIndex;
            }

            for (int z = 0; z < horizontalSize; z++) {
               for (int x = 0; x < horizontalSize; x++) {
                  int index = y * horizontalSize * horizontalSize + z * horizontalSize + x;
                  byte value = blockOutBufferData.get(index);
                  int chunkOffXxx = x >> 4 & BATCH_MASK;
                  int chunkOffZ = z >> 4 & BATCH_MASK;
                  int idx = (chunkOffZ << BATCH_SHIFT) + chunkOffXxx;
                  if (((long)writeProtectedMask & 1L << idx) == 0L) {
                     boolean needsFluidTick = (value & 128) != 0;
                     int blockIdx = value & 127;
                     BlockState blockState = clBlockStateMappings.getBlockState(blockIdx);
                     FlowschedAssertions.assertTrue(blockState != null);
                     if (blockState != AIR) {
                        ((PalettedContainerExtension)chunkSection[idx].getStates()).c2me$setUnsafe(x & 15, blockY & 15, z & 15, blockState);
                        if (needsFluidTick) {
                           ProtoChunk chunk = (ProtoChunk)chunks.get(this.startingPos.x + chunkOffXxx, this.startingPos.z + chunkOffZ);
                           mutablePos.set(x + chunk.getPos().getMinBlockX(), blockY, z + chunk.getPos().getMinBlockZ());
                           chunk.markPosForPostprocessing(mutablePos);
                        }
                     }
                  }
               }
            }
         }

         for (int chunkOffZ = 0; chunkOffZ < BATCH_SIZE; chunkOffZ++) {
            for (int chunkOffXxx = 0; chunkOffXxx < BATCH_SIZE; chunkOffXxx++) {
               int idx = (chunkOffZ << BATCH_SHIFT) + chunkOffXxx;
               if (((long)writeProtectedMask & 1L << idx) == 0L) {
                  chunkSection[idx].recalcBlockCounts();
                  chunkSection[idx].release();
                  ProtoChunk chunk = (ProtoChunk)chunks.get(this.startingPos.x + chunkOffXxx, this.startingPos.z + chunkOffZ);
                  Heightmap.primeHeightmaps(chunk, EnumSet.of(Types.OCEAN_FLOOR_WG, Types.WORLD_SURFACE_WG));
                  chunk.setStatus(ChunkStatus.NOISE);
               }
            }
         }
      }
   }
}
