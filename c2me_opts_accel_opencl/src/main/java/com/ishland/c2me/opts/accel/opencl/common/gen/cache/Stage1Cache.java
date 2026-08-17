package com.ishland.c2me.opts.accel.opencl.common.gen.cache;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ishland.c2me.base.common.GlobalExecutors;
import com.ishland.c2me.opts.accel.opencl.common.Config;
import com.ishland.c2me.opts.accel.opencl.common.compiler.OpenCLCGen;
import com.ishland.c2me.opts.accel.opencl.common.gen.CLDataUtil;
import com.ishland.c2me.opts.accel.opencl.common.gen.CLServerWorldContext;
import com.ishland.c2me.opts.accel.opencl.common.gen.OpenCLDevice;
import com.ishland.c2me.opts.accel.opencl.common.util.CLEventList;
import com.ishland.c2me.opts.accel.opencl.common.util.CLUtil;
import com.ishland.c2me.opts.accel.opencl.common.workarounds.Workarounds;
import com.ishland.c2me.opts.accel.opencl.common.util.FlowschedAssertions;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.longs.Long2ReferenceArrayMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongListIterator;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CL12;
import org.lwjgl.opencl.CLEventCallbackI;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Stage1Cache {
   private static final Logger LOGGER = LoggerFactory.getLogger(Stage1Cache.class);
   private static final int CACHE_CHUNK_WIDTH = Config.useSmallerBatches ? 8 : 16;
   private static final int CACHE_CHUNK_WIDTH_MASK = CACHE_CHUNK_WIDTH - 1;
   private static final int CACHE_CHUNK_WIDTH_SHIFT = Integer.numberOfTrailingZeros(CACHE_CHUNK_WIDTH);
   private static final int CACHE_WIDTH = CACHE_CHUNK_WIDTH << 2;
   private static final int CACHE_WIDTH_MASK = CACHE_WIDTH - 1;
   private static final int CACHE_WIDTH_SHIFT = Integer.numberOfTrailingZeros(CACHE_WIDTH);
   private final AsyncLoadingCache<Stage1Cache.CacheIndex, Stage1Cache.RawCacheEntry> cache;
   private final CLServerWorldContext worldContext;

   public Stage1Cache(CLServerWorldContext worldContext) {
      this.worldContext = Objects.requireNonNull(worldContext, "worldContext must not be null");
      this.cache = Caffeine.newBuilder().maximumSize(256L).executor(GlobalExecutors.asyncScheduler).buildAsync(this::asyncLoad0);
   }

   private CompletableFuture<Stage1Cache.RawCacheEntry> asyncLoad0(Stage1Cache.CacheIndex cacheIndex, Executor executor) {
      return this.worldContext
         .borrowCommandQueue()
         .thenCompose(
            pair -> CompletableFuture.<CompletableFuture<Stage1Cache.RawCacheEntry>>supplyAsync(
                     () -> this.execute0(cacheIndex, (Pair<OpenCLDevice.BorrowedCommandQueue, CLServerWorldContext.DeviceWithProgram>)pair),
                     ((OpenCLDevice.BorrowedCommandQueue)pair.left()).getDevice().getExecutor()
                  )
                  .thenCompose(Function.identity())
         );
   }

   @NotNull
   private CompletableFuture<Stage1Cache.RawCacheEntry> execute0(
      Stage1Cache.CacheIndex cacheIndex, Pair<OpenCLDevice.BorrowedCommandQueue, CLServerWorldContext.DeviceWithProgram> pair
   ) {
      CompletableFuture<Stage1Cache.RawCacheEntry> future = new CompletableFuture<>();
      OpenCLDevice.BorrowedCommandQueue commandQueue = (OpenCLDevice.BorrowedCommandQueue)pair.left();
      CLServerWorldContext.DeviceWithProgram deviceWithProgram = (CLServerWorldContext.DeviceWithProgram)pair.right();
      MemoryStack stack = MemoryStack.stackPush();

      try {
         IntBuffer errorCodeRet = stack.callocInt(1);
         CLBufferCache.BufferEntry surfaceHeightOutBuffer = deviceWithProgram.device()
            .getBufferCache()
            .allocate(
               CLBufferCache.Type.ESTIMATE_SURFACE_HEIGHT_RX,
               CACHE_WIDTH * CACHE_WIDTH * 4,
               size -> CL12.clCreateBuffer(commandQueue.getContext(), 2L, size, errorCodeRet)
            );
         CLUtil.checkCLError(errorCodeRet);
         ByteBuffer rwData = CLDataUtil.worldgen_data_root$createForFlatCacheOnly(
            new ChunkPos(cacheIndex.x << CACHE_CHUNK_WIDTH_SHIFT, cacheIndex.z << CACHE_CHUNK_WIDTH_SHIFT),
            CACHE_CHUNK_WIDTH,
            this.worldContext.getGeneratedCLSource(),
            null,
            false
         );
         CLBufferCache.BufferEntry rwBuffer = deviceWithProgram.device()
            .getBufferCache()
            .allocate(CLBufferCache.Type.GEN_STAGE1_RW_DATA, rwData.remaining(), size -> CL12.clCreateBuffer(commandQueue.getContext(), 1L, size, errorCodeRet));
         CLUtil.checkCLError(errorCodeRet);
         PointerBuffer eventRet = stack.callocPointer(1);
         CLEventList eventList = new CLEventList();
         LongList eventsToRelease = new LongArrayList();
         LongList kernelsToRelease = new LongArrayList();
         CLUtil.checkCLError(CL12.clEnqueueWriteBuffer(commandQueue.getCommandQueue(), rwBuffer.buffer(), false, 0L, rwData, null, eventRet));
         eventsToRelease.add(eventRet.get(0));
         eventList.add(eventRet.get(0));
         CLBufferCache.BufferEntry flatCacheOutBuffer;
         if (this.worldContext.getGeneratedCLSource().getFlatCachePrefills() != 0) {
            flatCacheOutBuffer = deviceWithProgram.device()
               .getBufferCache()
               .allocate(
                  CLBufferCache.Type.FLATCACHE_RX,
                  this.worldContext.getGeneratedCLSource().getFlatCachePrefills() * CACHE_WIDTH * CACHE_WIDTH * 8,
                  size -> CL12.clCreateBuffer(commandQueue.getContext(), 2L, size, errorCodeRet)
               );
            CLUtil.checkCLError(errorCodeRet);
            MemoryStack flatCacheOutBufferData = MemoryStack.stackPush();

            try {
               PointerBuffer workSize = stack.mallocPointer(2);
               workSize.put(0, (long)CACHE_WIDTH);
               workSize.put(1, (long)CACHE_WIDTH);
               workSize.rewind();
               PointerBuffer local = stack.callocPointer(2);
               local.put(0, 16L);
               local.put(1, 16L);
               local.rewind();
               PointerBuffer prevEvent = eventList.getEventWaitList(stack);
               eventList.clear();

               for (int i = 0; i < this.worldContext.getGeneratedCLSource().getFlatCachePrefills(); i++) {
                  long flatCachePrefillKernel = CL12.clCreateKernel(
                     deviceWithProgram.getProgram(OpenCLCGen.ProgramType.FLAT_CACHE_PREFILL), "df_flatcache_prefill_kernel_" + i, errorCodeRet
                  );
                  CLUtil.checkCLError(errorCodeRet);
                  CLUtil.checkCLError(CL12.clSetKernelArg1p(flatCachePrefillKernel, 0, deviceWithProgram.programConstDataBuffer()));
                  CLUtil.checkCLError(CL12.clSetKernelArg1p(flatCachePrefillKernel, 1, rwBuffer.buffer()));
                  CLUtil.checkCLError(CL12.clSetKernelArg1p(flatCachePrefillKernel, 2, flatCacheOutBuffer.buffer()));
                  CLUtil.checkCLError(
                     CL12.clEnqueueNDRangeKernel(commandQueue.getCommandQueue(), flatCachePrefillKernel, 2, null, workSize, local, prevEvent, eventRet)
                  );
                  eventsToRelease.add(eventRet.get(0));
                  eventList.add(eventRet.get(0));
                  kernelsToRelease.add(flatCachePrefillKernel);
               }
            } catch (Throwable var31) {
               if (flatCacheOutBufferData != null) {
                  try {
                     flatCacheOutBufferData.close();
                  } catch (Throwable var27) {
                     var31.addSuppressed(var27);
                  }
               }

               throw var31;
            }

            if (flatCacheOutBufferData != null) {
               flatCacheOutBufferData.close();
            }
         } else {
            flatCacheOutBuffer = null;
         }

         MemoryStack var33 = MemoryStack.stackPush();

         try {
            PointerBuffer workSize = stack.mallocPointer(3);
            workSize.put(0, (long)CACHE_WIDTH);
            workSize.put(1, (long)CACHE_WIDTH);
            workSize.rewind();
            PointerBuffer local = stack.callocPointer(3);
            local.put(0, 8L);
            local.put(1, 8L);
            local.rewind();
            PointerBuffer prevEvent = eventList.getEventWaitList(stack);
            eventList.clear();
            long estimateSurfaceHeightKernel = CL12.clCreateKernel(
               deviceWithProgram.getProgram(OpenCLCGen.ProgramType.ESTIMATE_SURFACE_HEIGHT),
               "chunkNoiseSampler_estimateSurfaceHeight_prefill_indep",
               errorCodeRet
            );
            CLUtil.checkCLError(errorCodeRet);
            CLUtil.checkCLError(CL12.clSetKernelArg1p(estimateSurfaceHeightKernel, 0, deviceWithProgram.programConstDataBuffer()));
            CLUtil.checkCLError(CL12.clSetKernelArg1p(estimateSurfaceHeightKernel, 1, rwBuffer.buffer()));
            CLUtil.checkCLError(CL12.clSetKernelArg1p(estimateSurfaceHeightKernel, 2, surfaceHeightOutBuffer.buffer()));
            CLUtil.checkCLError(CL12.clSetKernelArg1i(estimateSurfaceHeightKernel, 3, cacheIndex.x << CACHE_CHUNK_WIDTH_SHIFT));
            CLUtil.checkCLError(CL12.clSetKernelArg1i(estimateSurfaceHeightKernel, 4, cacheIndex.z << CACHE_CHUNK_WIDTH_SHIFT));
            CLUtil.checkCLError(CL12.clSetKernelArg1i(estimateSurfaceHeightKernel, 5, CACHE_WIDTH));
            CLUtil.checkCLError(
               CL12.clEnqueueNDRangeKernel(commandQueue.getCommandQueue(), estimateSurfaceHeightKernel, 2, null, workSize, local, prevEvent, eventRet)
            );
            eventsToRelease.add(eventRet.get(0));
            eventList.add(eventRet.get(0));
            kernelsToRelease.add(estimateSurfaceHeightKernel);
         } catch (Throwable var30) {
            if (var33 != null) {
               try {
                  var33.close();
               } catch (Throwable var26) {
                  var30.addSuppressed(var26);
               }
            }

            throw var30;
         }

         if (var33 != null) {
            var33.close();
         }

         IntBuffer surfaceHeightBufferData = MemoryUtil.memAllocInt(CACHE_WIDTH * CACHE_WIDTH);
         PointerBuffer prevEvent = eventList.getEventWaitList(stack);
         eventList.clear();
         DoubleBuffer flatCacheOutBufferData;
         if (this.worldContext.getGeneratedCLSource().getFlatCachePrefills() != 0) {
            FlowschedAssertions.assertTrue(flatCacheOutBuffer != null);

            assert flatCacheOutBuffer != null;

            flatCacheOutBufferData = MemoryUtil.memAllocDouble(this.worldContext.getGeneratedCLSource().getFlatCachePrefills() * CACHE_WIDTH * CACHE_WIDTH);
            MemoryStack var41 = MemoryStack.stackPush();

            try {
               CLUtil.checkCLError(
                  CL12.clEnqueueReadBuffer(commandQueue.getCommandQueue(), flatCacheOutBuffer.buffer(), false, 0L, flatCacheOutBufferData, prevEvent, eventRet)
               );
               eventsToRelease.add(eventRet.get(0));
               eventList.add(eventRet.get(0));
            } catch (Throwable var29) {
               if (var41 != null) {
                  try {
                     var41.close();
                  } catch (Throwable var25) {
                     var29.addSuppressed(var25);
                  }
               }

               throw var29;
            }

            if (var41 != null) {
               var41.close();
            }
         } else {
            flatCacheOutBufferData = null;
         }

         MemoryStack var42 = MemoryStack.stackPush();

         try {
            CLUtil.checkCLError(
               CL12.clEnqueueReadBuffer(
                  commandQueue.getCommandQueue(), surfaceHeightOutBuffer.buffer(), false, 0L, surfaceHeightBufferData, prevEvent, eventRet
               )
            );
            eventsToRelease.add(eventRet.get(0));
            eventList.add(eventRet.get(0));
         } catch (Throwable var28) {
            if (var42 != null) {
               try {
                  var42.close();
               } catch (Throwable var24) {
                  var28.addSuppressed(var24);
               }
            }

            throw var28;
         }

         if (var42 != null) {
            var42.close();
         }

         if (Config.doExplicitFlushes || deviceWithProgram.device().getWorkarounds().contains(Workarounds.Reference.REQUIRE_EXPLICIT_FLUSHES)) {
            CLUtil.checkCLError(CL12.clFlush(commandQueue.getCommandQueue()));
         }

         AtomicInteger counter = new AtomicInteger(eventList.size());
         CLEventCallbackI callback = (event1, event_command_exec_status, user_data) -> {
            if (counter.decrementAndGet() == 0) {
               GlobalExecutors.executor.execute(() -> {
                  try {
                     try {
                        if (event_command_exec_status != 0) {
                           LOGGER.error("OpenCL command failed: {}", event_command_exec_status);
                           future.completeExceptionally(new RuntimeException("OpenCL command failed: " + event_command_exec_status));
                        } else {
                           int[] surfaceHeight = new int[CACHE_WIDTH * CACHE_WIDTH];
                           surfaceHeightBufferData.get(0, surfaceHeight);
                           double[] flatCache = new double[this.worldContext.getGeneratedCLSource().getFlatCachePrefills() * CACHE_WIDTH * CACHE_WIDTH];
                           if (flatCacheOutBufferData != null) {
                              flatCacheOutBufferData.get(0, flatCache);
                           }

                           future.complete(new Stage1Cache.RawCacheEntry(surfaceHeight, flatCache));
                        }
                     } finally {
                        deviceWithProgram.device().getExecutor().execute(() -> {
                           deviceWithProgram.device().getBufferCache().returnBuffer(CLBufferCache.Type.ESTIMATE_SURFACE_HEIGHT_RX, surfaceHeightOutBuffer);
                           deviceWithProgram.device().getBufferCache().returnBuffer(CLBufferCache.Type.GEN_STAGE1_RW_DATA, rwBuffer);
                           if (flatCacheOutBuffer != null) {
                              deviceWithProgram.device().getBufferCache().returnBuffer(CLBufferCache.Type.FLATCACHE_RX, flatCacheOutBuffer);
                           }

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
                        MemoryUtil.memFree(surfaceHeightBufferData);
                        if (flatCacheOutBufferData != null) {
                           MemoryUtil.memFree(flatCacheOutBufferData);
                        }

                        MemoryUtil.memFree(rwData);
                        commandQueue.close();
                     }
                  } catch (Throwable var19x) {
                     future.completeExceptionally(var19x);
                  }
               });
            }
         };
         LongListIterator iterator = eventList.iterator();

         while (iterator.hasNext()) {
            deviceWithProgram.device().getEventCallbackManager().registerCallback(iterator.nextLong(), 0, callback);
         }
      } catch (Throwable var32) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var23) {
               var32.addSuppressed(var23);
            }
         }

         throw var32;
      }

      if (stack != null) {
         stack.close();
      }

      future.exceptionally(throwable -> {
         LOGGER.error("Stage1Cache threw exception", throwable);
         return null;
      });
      return future.thenApply(Function.identity())
         .orTimeout(120L, TimeUnit.SECONDS)
         .exceptionallyCompose(
            throwable -> throwable instanceof TimeoutException
                  ? CompletableFuture.failedFuture(
                     new TimeoutException(
                        String.format("Stage1Cache timed out for batch [%d, %d] on %s", cacheIndex.x, cacheIndex.z, deviceWithProgram.device().toString())
                     )
                  )
                  : CompletableFuture.failedFuture(throwable)
         );
   }

   public CompletableFuture<Stage1Cache.AreaCacheEntry> getChunkCache(int chunkX, int chunkZ) {
      return this.getAreaCache0(chunkX, chunkZ, 1, 1)
         .thenApply(entry -> new Stage1Cache.AreaCacheEntry(chunkX, chunkZ, 1, 1, entry.surfaceHeights(), entry.flatCaches()));
   }

   public CompletableFuture<Stage1Cache.AreaCacheEntry> getAreaCache(int startChunkX, int startChunkZ, int sizeX, int sizeZ) {
      return this.getAreaCache0(startChunkX, startChunkZ, sizeX, sizeZ)
         .thenApply(entry -> new Stage1Cache.AreaCacheEntry(startChunkX, startChunkZ, sizeX, sizeZ, entry.surfaceHeights(), entry.flatCaches()));
   }

   private CompletableFuture<Stage1Cache.RawCacheEntry> getAreaCache0(int startChunkX, int startChunkZ, int sizeX, int sizeZ) {
      int startCacheX = startChunkX - 4 >> CACHE_CHUNK_WIDTH_SHIFT;
      int startCacheZ = startChunkZ - 4 >> CACHE_CHUNK_WIDTH_SHIFT;
      int endCacheX = startChunkX + sizeX - 1 + 4 >> CACHE_CHUNK_WIDTH_SHIFT;
      int endCacheZ = startChunkZ + sizeZ - 1 + 4 >> CACHE_CHUNK_WIDTH_SHIFT;
      Long2ReferenceArrayMap<CompletableFuture<Stage1Cache.RawCacheEntry>> futures = new Long2ReferenceArrayMap(
         (endCacheX - startCacheX + 1) * (endCacheZ - startCacheZ + 1)
      );

      for (int cacheX = startCacheX; cacheX <= endCacheX; cacheX++) {
         for (int cacheZ = startCacheZ; cacheZ <= endCacheZ; cacheZ++) {
            Stage1Cache.CacheIndex cacheIndex = new Stage1Cache.CacheIndex(cacheX, cacheZ);
            futures.put(cacheIndex.toLong(), this.cache.get(cacheIndex));
         }
      }

      return CompletableFuture.allOf((CompletableFuture<?>[])futures.values().toArray(CompletableFuture[]::new))
         .thenApply(
            var6x -> {
               int surfaceHeightSizeX = 36 + 4 * (sizeX - 1);
               int surfaceHeightSizeZ = 36 + 4 * (sizeZ - 1);
               int[] surfaceHeight = new int[surfaceHeightSizeX * surfaceHeightSizeZ];

               for (int relX = 0; relX < surfaceHeightSizeX; relX++) {
                  for (int relZ = 0; relZ < surfaceHeightSizeZ; relZ++) {
                     int cacheXxx = (startChunkX - 4 << 2) + relX >> CACHE_WIDTH_SHIFT;
                     int cacheZxx = (startChunkZ - 4 << 2) + relZ >> CACHE_WIDTH_SHIFT;
                     int cacheRelX = (startChunkX - 4 << 2) + relX & CACHE_WIDTH_MASK;
                     int cacheRelZ = (startChunkZ - 4 << 2) + relZ & CACHE_WIDTH_MASK;
                     int[] cacheData = ((Stage1Cache.RawCacheEntry)((CompletableFuture)futures.get(Stage1Cache.CacheIndex.toLong(cacheXxx, cacheZxx))).join())
                        .surfaceHeights();
                     surfaceHeight[relX * surfaceHeightSizeZ + relZ] = cacheData[(cacheRelX << CACHE_WIDTH_SHIFT) + cacheRelZ];
                  }
               }

               int flatCachePrefills = this.worldContext.getGeneratedCLSource().getFlatCachePrefills();
               int flatCacheSizeX = 5 + 4 * (sizeX - 1);
               int flatCacheSizeZ = 5 + 4 * (sizeZ - 1);
               int cacheIndexScale = flatCacheSizeX * flatCacheSizeZ;
               double[] flatCache = new double[flatCachePrefills * flatCacheSizeX * flatCacheSizeZ];

               for (int cacheIndexx = 0; cacheIndexx < flatCachePrefills; cacheIndexx++) {
                  for (int relX = 0; relX < flatCacheSizeX; relX++) {
                     for (int relZ = 0; relZ < flatCacheSizeZ; relZ++) {
                        int cacheXx = (startChunkX << 2) + relX >> CACHE_WIDTH_SHIFT;
                        int cacheZx = (startChunkZ << 2) + relZ >> CACHE_WIDTH_SHIFT;
                        int cacheRelX = (startChunkX << 2) + relX & CACHE_WIDTH_MASK;
                        int cacheRelZ = (startChunkZ << 2) + relZ & CACHE_WIDTH_MASK;
                        double[] cacheData = ((Stage1Cache.RawCacheEntry)((CompletableFuture)futures.get(Stage1Cache.CacheIndex.toLong(cacheXx, cacheZx)))
                              .join())
                           .flatCaches();
                        flatCache[cacheIndexx * cacheIndexScale + relX * flatCacheSizeZ + relZ] = cacheData[(cacheIndexx << (CACHE_WIDTH_SHIFT << 1))
                           + (cacheRelX << CACHE_WIDTH_SHIFT)
                           + cacheRelZ];
                     }
                  }
               }

               return new Stage1Cache.RawCacheEntry(surfaceHeight, flatCache);
            }
         );
   }

   public static record AreaCacheEntry(int chunkX, int chunkZ, int sizeX, int sizeZ, int[] surfaceHeights, double[] flatCaches) {
   }

   private static record CacheIndex(int x, int z) {
      public CacheIndex(long value) {
         this((int)(value >> 32), (int)value);
      }

      public long toLong() {
         return toLong(this.x, this.z);
      }

      public static long toLong(int x, int z) {
         return (long)x << 32 | (long)z & 4294967295L;
      }
   }

   public static record RawCacheEntry(int[] surfaceHeights, double[] flatCaches) {
   }
}
