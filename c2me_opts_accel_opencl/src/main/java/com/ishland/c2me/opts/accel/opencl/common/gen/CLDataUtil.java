package com.ishland.c2me.opts.accel.opencl.common.gen;

import com.ishland.c2me.opts.accel.opencl.common.util.MemoryUtil;
import com.ishland.c2me.opts.accel.opencl.mixin.access.INoiseChunkGenerator;
import com.ishland.c2me.opts.accel.opencl.common.compiler.GeneratedCLSource;
import com.ishland.c2me.opts.accel.opencl.common.compiler.OpenCLCGen;
import com.ishland.c2me.opts.accel.opencl.common.compiler.emitters.misc.CLBlockStateMappings;
import com.ishland.c2me.opts.accel.opencl.common.gen.cache.Stage1Cache;
import com.ishland.c2me.opts.accel.opencl.common.util.OpenCLStructs;
import com.ishland.c2me.opts.natives_math.common.BindingsTemplate;
import com.ishland.c2me.opts.accel.opencl.common.util.FlowschedAssertions;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;
import it.unimi.dsi.fastutil.objects.Reference2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2IntMap.Entry;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.function.Function;
import net.minecraft.core.QuartPos;
import net.minecraft.util.Mth;
import com.ishland.c2me.opts.accel.opencl.common.util.StaticCache2D;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.Aquifer.FluidPicker;
import net.minecraft.world.level.levelgen.DensityFunctions.BeardifierMarker;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public class CLDataUtil {

   private static void copyDoublesToBytes(double[] src, byte[] dst) {
      ByteBuffer bb = ByteBuffer.wrap(dst).order(ByteOrder.nativeOrder());
      for (double d : src) bb.putDouble(d);
   }

   private static void copyIntsToBytes(int[] src, byte[] dst) {
      ByteBuffer bb = ByteBuffer.wrap(dst).order(ByteOrder.nativeOrder());
      for (int i : src) bb.putInt(i);
   }

   public static ByteBuffer worldgen_data_root$createForFlatCacheOnly(
      ChunkPos base, int chunkSize, GeneratedCLSource generatedCLSource, double[] flatCachePrefilled, boolean extendByOne
   ) {
      if (generatedCLSource == null) {
         throw new IllegalStateException("Generated CL source not found");
      } else {
         record OffsetAndData(int offset, byte[] data) {
         }

         OffsetAndData[] allocatedOffsets = new OffsetAndData[generatedCLSource.getGlobalDynamicDataOffsets().size()];
         int currentTail = MemoryUtil.roundUp(128 + generatedCLSource.getGlobalDynamicDataOffsets().size() * 4, 32);
         ObjectBidirectionalIterator byteBuffer = generatedCLSource.getGlobalDynamicDataOffsets().reference2IntEntrySet().iterator();

         while (byteBuffer.hasNext()) {
            Entry<Object> entry = (Entry<Object>) byteBuffer.next();
            Object key = entry.getKey();
            int index = entry.getIntValue();
            FlowschedAssertions.assertTrue(allocatedOffsets[index] == null);
            if (key == OpenCLCGen.MARKER_localOffsetTable) {
               allocatedOffsets[index] = new OffsetAndData(0, null);
            } else if (key instanceof ConstantBlob blob) {
               int offset = MemoryUtil.roundUp(currentTail, blob.alignment);
               currentTail = offset + blob.data.length;
               allocatedOffsets[index] = new OffsetAndData(offset, blob.data);
            } else if (key == BeardifierMarker.INSTANCE) {
               allocatedOffsets[index] = new OffsetAndData(0, null);
            } else if (key == OpenCLCGen.MARKER_estimateSurfaceHeightCache) {
               allocatedOffsets[index] = new OffsetAndData(0, null);
            } else if (key == OpenCLCGen.MARKER_aquifer) {
               allocatedOffsets[index] = new OffsetAndData(0, null);
            } else if (key == OpenCLCGen.MARKER_fluidLevelSampler) {
               allocatedOffsets[index] = new OffsetAndData(0, null);
            } else if (key == OpenCLCGen.MARKER_oreVeinRandom) {
               allocatedOffsets[index] = new OffsetAndData(0, null);
            } else if (key == OpenCLCGen.MARKER_cacheLike_interpolator) {
               allocatedOffsets[index] = new OffsetAndData(0, null);
            } else if (key == OpenCLCGen.MARKER_cacheLike_flatCache) {
               int offset = MemoryUtil.roundUp(currentTail, 8);
               int bufSize = generatedCLSource.getFlatCachePrefills() * Mth.square(chunkSize * 4 + 1) * 8;
               currentTail = offset + bufSize;
               if (flatCachePrefilled != null) {
                  byte[] data = new byte[flatCachePrefilled.length * 8];
                  copyDoublesToBytes(flatCachePrefilled, data);
                  FlowschedAssertions.assertTrue(data.length == bufSize);
                  allocatedOffsets[index] = new OffsetAndData(offset, data);
               } else {
                  allocatedOffsets[index] = new OffsetAndData(offset, null);
               }
            } else {
               if (key != OpenCLCGen.MARKER_cacheLike_cache2d) {
                  throw new UnsupportedOperationException("Unsupported key type " + key.getClass().getName());
               }

               allocatedOffsets[index] = new OffsetAndData(0, null);
            }
         }

         ByteBuffer segment = org.lwjgl.system.MemoryUtil.memAlloc(MemoryUtil.roundUp(currentTail, 32));
         OpenCLStructs.putInt(segment, OpenCLStructs.worldgen_params$startBiomeX, QuartPos.fromSection(base.x));
         OpenCLStructs.putInt(segment, OpenCLStructs.worldgen_params$startBiomeZ, QuartPos.fromSection(base.z));
         OpenCLStructs.putInt(segment, OpenCLStructs.worldgen_params$sizeBiomeX, chunkSize * 4 - (extendByOne ? 0 : 1));
         OpenCLStructs.putInt(segment, OpenCLStructs.worldgen_params$sizeBiomeZ, chunkSize * 4 - (extendByOne ? 0 : 1));
         OpenCLStructs.putInt(segment, OpenCLStructs.worldgen_params$startCellX, Integer.MAX_VALUE);
         OpenCLStructs.putInt(segment, OpenCLStructs.worldgen_params$startCellY, Integer.MAX_VALUE);
         OpenCLStructs.putInt(segment, OpenCLStructs.worldgen_params$startCellZ, Integer.MAX_VALUE);
         OpenCLStructs.putInt(segment, OpenCLStructs.worldgen_params$sizeCellX, 0);
         OpenCLStructs.putInt(segment, OpenCLStructs.worldgen_params$sizeCellY, 0);
         OpenCLStructs.putInt(segment, OpenCLStructs.worldgen_params$sizeCellZ, 0);
         OpenCLStructs.putInt(segment, OpenCLStructs.worldgen_params$estimateSurfaceHeight_startBiomeX, Integer.MAX_VALUE);
         OpenCLStructs.putInt(segment, OpenCLStructs.worldgen_params$estimateSurfaceHeight_startBiomeZ, Integer.MAX_VALUE);
         OpenCLStructs.putInt(segment, OpenCLStructs.worldgen_params$estimateSurfaceHeight_sizeBiomeX, 0);
         OpenCLStructs.putInt(segment, OpenCLStructs.worldgen_params$estimateSurfaceHeight_sizeBiomeZ, 0);
         OpenCLStructs.putInt(segment, OpenCLStructs.worldgen_params$cache2d_startX, Integer.MAX_VALUE);
         OpenCLStructs.putInt(segment, OpenCLStructs.worldgen_params$cache2d_startZ, Integer.MAX_VALUE);
         OpenCLStructs.putInt(segment, OpenCLStructs.worldgen_params$cache2d_sizeX, 0);
         OpenCLStructs.putInt(segment, OpenCLStructs.worldgen_params$cache2d_sizeZ, 0);
         OpenCLStructs.putInt(segment, OpenCLStructs.worldgen_params$offset_estimateSurfaceHeight, 0);
         OpenCLStructs.putInt(segment, OpenCLStructs.worldgen_params$genConfig_defaultBlock, 0);
         OpenCLStructs.putInt(segment, OpenCLStructs.worldgen_params$genConfig_defaultFluid, 0);
         OpenCLStructs.putInt(segment, OpenCLStructs.worldgen_params$offset_aquifer, 0);
         OpenCLStructs.putInt(segment, OpenCLStructs.worldgen_params$offset_fluidLevelSampler, 0);
         OpenCLStructs.putInt(segment, OpenCLStructs.worldgen_params$offset_oreVeinRandom, 0);
         int i = 0;

         for (int allocatedOffsetsLength = allocatedOffsets.length; i < allocatedOffsetsLength; i++) {
            OffsetAndData allocatedOffset = allocatedOffsets[i];
            FlowschedAssertions.assertTrue(allocatedOffset != null);
            segment.putInt(128 + i * 4, allocatedOffset.offset);
            if (allocatedOffset.offset != 0 && allocatedOffset.data != null) {
               segment.put(allocatedOffset.offset, allocatedOffset.data, 0, allocatedOffset.data.length);
            }
         }

         return segment;
      }
   }

   public static ByteBuffer worldgen_data_root$createForArea(
      ChunkPos basePos,
      int horizontalChunkSize,
      StaticCache2D<ProtoChunk> regionArray,
      NoiseBasedChunkGenerator generator,
      RandomState noiseConfig,
      StaticCache2D<StructureManager> structureAccessors,
      CLBlockStateMappings mappings,
      GeneratedCLSource generatedCLSource,
      Stage1Cache.AreaCacheEntry stage1Cache
   ) {
      if (generatedCLSource == null) {
         throw new IllegalStateException("Generated CL source not found");
      } else {
         NoiseGeneratorSettings settings = (NoiseGeneratorSettings) generator.generatorSettings().value();
         FluidPicker fluidLevelSampler = (FluidPicker) ((INoiseChunkGenerator) generator).getFluidLevelSampler().get();
         NoiseSettings generationShapeConfig = settings.noiseSettings();
         int horizontalCellCount = horizontalChunkSize * 16 / generationShapeConfig.getCellWidth();
         int verticalCellCount = Mth.floorDiv(generationShapeConfig.height(), generationShapeConfig.getCellHeight());
         FlowschedAssertions.assertTrue(horizontalCellCount * generationShapeConfig.getCellWidth() == horizontalChunkSize * 16);

         record OffsetAndData(int offset, byte[] data) {
         }

         Function<ProtoChunk, NoiseChunk> toChunkNoiseSampler = chunkx -> chunkx.getOrCreateNoiseChunk(chunkxx -> {
            StructureManager structureAccessor = (StructureManager) structureAccessors.get(chunkxx.getPos().x, chunkxx.getPos().z);
            return ((INoiseChunkGenerator) generator).invokeCreateChunkNoiseSampler(chunkxx, structureAccessor, Blender.empty(), noiseConfig);
         });
         OffsetAndData[] allocatedOffsets = new OffsetAndData[generatedCLSource.getGlobalDynamicDataOffsets().size()];
         int currentTail = MemoryUtil.roundUp(128 + generatedCLSource.getGlobalDynamicDataOffsets().size() * 4, 32);
         ObjectBidirectionalIterator byteBuffer = generatedCLSource.getGlobalDynamicDataOffsets().reference2IntEntrySet().iterator();

         while (byteBuffer.hasNext()) {
            Entry<Object> entry = (Entry<Object>) byteBuffer.next();
            Object key = entry.getKey();
            int index = entry.getIntValue();
            FlowschedAssertions.assertTrue(allocatedOffsets[index] == null);
            if (key == OpenCLCGen.MARKER_localOffsetTable) {
               allocatedOffsets[index] = new OffsetAndData(0, null);
            } else if (key instanceof ConstantBlob blob) {
               int offset = MemoryUtil.roundUp(currentTail, blob.alignment);
               currentTail = offset + blob.data.length;
               allocatedOffsets[index] = new OffsetAndData(offset, blob.data);
            } else if (key == BeardifierMarker.INSTANCE) {
               int offset = MemoryUtil.roundUp(currentTail, 4);
               byte[] data;
               ByteBuffer segment = OpenCLStructs.sws_index$createForArea(
                  basePos, regionArray, toChunkNoiseSampler, horizontalChunkSize, horizontalChunkSize
               );
               data = new byte[segment.capacity()];
               segment.get(0, data, 0, data.length);
               currentTail = offset + data.length;
               allocatedOffsets[index] = new OffsetAndData(offset, data);
            } else if (key == OpenCLCGen.MARKER_estimateSurfaceHeightCache) {
               if (stage1Cache != null) {
                  int offset = MemoryUtil.roundUp(currentTail, 4);
                  currentTail = offset + stage1Cache.surfaceHeights().length * 4;
                  byte[] datax = new byte[stage1Cache.surfaceHeights().length * 4];
                  copyIntsToBytes(stage1Cache.surfaceHeights(), datax);
                  allocatedOffsets[index] = new OffsetAndData(offset, datax);
               } else {
                  allocatedOffsets[index] = new OffsetAndData(0, null);
               }
            } else if (key == OpenCLCGen.MARKER_aquifer) {
               if (settings.isAquifersEnabled()) {
                  int offset = MemoryUtil.roundUp(currentTail, 8);
                  int startX = Math.floorDiv(basePos.getMinBlockX() - 5, 16) + 0;
                  int startY = Math.floorDiv(generationShapeConfig.minY() + 1, 12) - 1;
                  int startZ = Math.floorDiv(basePos.getMinBlockZ() - 5, 16) + 0;
                  ChunkPos endChunkPos = new ChunkPos(basePos.x + horizontalChunkSize - 1, basePos.z + horizontalChunkSize - 1);
                  int endX = Math.floorDiv(endChunkPos.getMaxBlockX() + 5 - 1, 16) + 1;
                  int endY = Math.floorDiv(generationShapeConfig.minY() + generationShapeConfig.height() - 1, 12) + 1;
                  int endZ = Math.floorDiv(endChunkPos.getMaxBlockZ() + 5 - 1, 16) + 1;
                  int samplingYLowPassCutoff = generationShapeConfig.minY() + generationShapeConfig.height();
                  ByteBuffer segment = OpenCLStructs.aquifer_data$create(
                     startX,
                     startY,
                     startZ,
                     endX - startX + 1,
                     endY - startY + 1,
                     endZ - startZ + 1,
                     samplingYLowPassCutoff,
                     noiseConfig.aquiferRandom()
                  );
                  byte[] datax = new byte[segment.capacity()];
                  segment.get(0, datax, 0, datax.length);
                  currentTail = offset + datax.length;
                  allocatedOffsets[index] = new OffsetAndData(offset, datax);
               } else {
                  allocatedOffsets[index] = new OffsetAndData(0, null);
               }
            } else if (key == OpenCLCGen.MARKER_fluidLevelSampler) {
               int offset = MemoryUtil.roundUp(currentTail, 4);
               ByteBuffer segment = OpenCLStructs.fluidLevelSamplerCreate(
                  generationShapeConfig.minY(), generationShapeConfig.height(), fluidLevelSampler, mappings
               );
               byte[] dataxx = new byte[segment.capacity()];
               segment.get(0, dataxx, 0, dataxx.length);
               currentTail = offset + dataxx.length;
               allocatedOffsets[index] = new OffsetAndData(offset, dataxx);
            } else if (key == OpenCLCGen.MARKER_oreVeinRandom) {
               if (settings.oreVeinsEnabled()) {
                  int offset = MemoryUtil.roundUp(currentTail, 8);
                  ByteBuffer segment = org.lwjgl.system.MemoryUtil.memAlloc(24);
                  OpenCLStructs.setRandomState(segment, noiseConfig.oreRandom());
                  byte[] dataxxx = new byte[segment.capacity()];
                  segment.get(0, dataxxx, 0, dataxxx.length);
                  currentTail = offset + dataxxx.length;
                  allocatedOffsets[index] = new OffsetAndData(offset, dataxxx);
               } else {
                  allocatedOffsets[index] = new OffsetAndData(0, null);
               }
            } else if (key == OpenCLCGen.MARKER_cacheLike_interpolator) {
               int offset = MemoryUtil.roundUp(currentTail, 8);
               currentTail = offset
                  + generatedCLSource.getInterpolatorPrefills() * (horizontalCellCount + 1) * (verticalCellCount + 1) * (horizontalCellCount + 1) * 8;
               allocatedOffsets[index] = new OffsetAndData(offset, null);
            } else if (key == OpenCLCGen.MARKER_cacheLike_flatCache) {
               int offset = MemoryUtil.roundUp(currentTail, 8);
               int bufSize = generatedCLSource.getFlatCachePrefills() * Mth.square(horizontalChunkSize * 4 + 1) * 8;
               if (stage1Cache != null) {
                  currentTail = offset + bufSize;
                  byte[] dataxxxx = new byte[stage1Cache.flatCaches().length * 8];
                  copyDoublesToBytes(stage1Cache.flatCaches(), dataxxxx);
                  FlowschedAssertions.assertTrue(dataxxxx.length == bufSize);
                  allocatedOffsets[index] = new OffsetAndData(offset, dataxxxx);
               } else {
                  currentTail = offset + bufSize;
                  allocatedOffsets[index] = new OffsetAndData(offset, null);
               }
            } else {
               if (key != OpenCLCGen.MARKER_cacheLike_cache2d) {
                  throw new UnsupportedOperationException("Unsupported key type " + key.getClass().getName());
               }

               int offset = MemoryUtil.roundUp(currentTail, 8);
               currentTail = offset + generatedCLSource.getCache2dPrefills() * Mth.square(horizontalChunkSize * 16) * 8;
               allocatedOffsets[index] = new OffsetAndData(offset, null);
            }
         }

         ByteBuffer segment = org.lwjgl.system.MemoryUtil.memAlloc(MemoryUtil.roundUp(currentTail, 32));
         OpenCLStructs.putInt(segment, OpenCLStructs.worldgen_params$startBiomeX, QuartPos.fromSection(basePos.x));
         OpenCLStructs.putInt(segment, OpenCLStructs.worldgen_params$startBiomeZ, QuartPos.fromSection(basePos.z));
         OpenCLStructs.putInt(segment, OpenCLStructs.worldgen_params$sizeBiomeX, horizontalChunkSize << 2);
         OpenCLStructs.putInt(segment, OpenCLStructs.worldgen_params$sizeBiomeZ, horizontalChunkSize << 2);
         OpenCLStructs.putInt(segment, OpenCLStructs.worldgen_params$startCellX, Math.floorDiv(basePos.getMinBlockX(), generationShapeConfig.getCellWidth()));
         OpenCLStructs.putInt(segment, OpenCLStructs.worldgen_params$startCellY, Math.floorDiv(generationShapeConfig.minY(), generationShapeConfig.getCellHeight()));
         OpenCLStructs.putInt(segment, OpenCLStructs.worldgen_params$startCellZ, Math.floorDiv(basePos.getMinBlockZ(), generationShapeConfig.getCellWidth()));
         OpenCLStructs.putInt(segment, OpenCLStructs.worldgen_params$sizeCellX, horizontalCellCount);
         OpenCLStructs.putInt(segment, OpenCLStructs.worldgen_params$sizeCellY, verticalCellCount);
         OpenCLStructs.putInt(segment, OpenCLStructs.worldgen_params$sizeCellZ, horizontalCellCount);
         OpenCLStructs.putInt(segment, OpenCLStructs.worldgen_params$estimateSurfaceHeight_startBiomeX, basePos.x - 4 << 2);
         OpenCLStructs.putInt(segment, OpenCLStructs.worldgen_params$estimateSurfaceHeight_startBiomeZ, basePos.z - 4 << 2);
         OpenCLStructs.putInt(segment, OpenCLStructs.worldgen_params$estimateSurfaceHeight_sizeBiomeX, 32 + 4 * horizontalChunkSize);
         OpenCLStructs.putInt(segment, OpenCLStructs.worldgen_params$estimateSurfaceHeight_sizeBiomeZ, 32 + 4 * horizontalChunkSize);
         OpenCLStructs.putInt(segment, OpenCLStructs.worldgen_params$cache2d_startX, basePos.getMinBlockX());
         OpenCLStructs.putInt(segment, OpenCLStructs.worldgen_params$cache2d_startZ, basePos.getMinBlockZ());
         OpenCLStructs.putInt(segment, OpenCLStructs.worldgen_params$cache2d_sizeX, horizontalChunkSize * 16);
         OpenCLStructs.putInt(segment, OpenCLStructs.worldgen_params$cache2d_sizeZ, horizontalChunkSize * 16);
         OpenCLStructs.putInt(segment, OpenCLStructs.worldgen_params$offset_estimateSurfaceHeight,
            allocatedOffsets[generatedCLSource.getGlobalDynamicDataOffsets().getInt(OpenCLCGen.MARKER_estimateSurfaceHeightCache)].offset);
         OpenCLStructs.putInt(segment, OpenCLStructs.worldgen_params$genConfig_defaultBlock, mappings.toId(settings.defaultBlock()));
         OpenCLStructs.putInt(segment, OpenCLStructs.worldgen_params$genConfig_defaultFluid, mappings.toId(settings.defaultFluid()));
         OpenCLStructs.putInt(segment, OpenCLStructs.worldgen_params$offset_aquifer,
            allocatedOffsets[generatedCLSource.getGlobalDynamicDataOffsets().getInt(OpenCLCGen.MARKER_aquifer)].offset);
         OpenCLStructs.putInt(segment, OpenCLStructs.worldgen_params$offset_fluidLevelSampler,
            allocatedOffsets[generatedCLSource.getGlobalDynamicDataOffsets().getInt(OpenCLCGen.MARKER_fluidLevelSampler)].offset);
         OpenCLStructs.putInt(segment, OpenCLStructs.worldgen_params$offset_oreVeinRandom,
            allocatedOffsets[generatedCLSource.getGlobalDynamicDataOffsets().getInt(OpenCLCGen.MARKER_oreVeinRandom)].offset);
         int i = 0;

         for (int allocatedOffsetsLength = allocatedOffsets.length; i < allocatedOffsetsLength; i++) {
            OffsetAndData allocatedOffset = allocatedOffsets[i];
            FlowschedAssertions.assertTrue(allocatedOffset != null);
            segment.putInt(128 + i * 4, allocatedOffset.offset);
            if (allocatedOffset.offset != 0 && allocatedOffset.data != null) {
               segment.put(allocatedOffset.offset, allocatedOffset.data, 0, allocatedOffset.data.length);
            }
         }

         return segment;
      }
   }

   public static Reference2IntLinkedOpenHashMap<Object> transformGlobalDynamicDataOffsets(Reference2IntLinkedOpenHashMap<Object> globalDynamicDataOffsets) {
      Reference2IntLinkedOpenHashMap<Object> newMap = new Reference2IntLinkedOpenHashMap();
      ObjectBidirectionalIterator<Entry<Object>> iterator = globalDynamicDataOffsets.reference2IntEntrySet().fastIterator();

      while (iterator.hasNext()) {
         Entry<Object> entry = (Entry<Object>) iterator.next();
         Object key = entry.getKey();
         if (key instanceof BlendedNoise sampler) {
            ByteBuffer memorySegment = BindingsTemplate.interpolated_noise_sampler$create(sampler, true);
            byte[] bytes = new byte[memorySegment.capacity()];
            memorySegment.get(0, bytes, 0, bytes.length);
            newMap.put(new ConstantBlob(bytes, 8), entry.getIntValue());
         } else if (key instanceof NormalNoise sampler) {
            byte[] bytes = OpenCLCGen.bytes(sampler);
            newMap.put(new ConstantBlob(bytes, 8), entry.getIntValue());
         } else {
            newMap.put(key, entry.getIntValue());
         }
      }

      return newMap;
   }

   public static record ConstantBlob(byte[] data, int alignment) {
   }
}
