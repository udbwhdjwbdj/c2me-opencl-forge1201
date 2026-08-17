package com.ishland.c2me.opts.accel.opencl.common.util;

import com.google.common.collect.Iterators;
import com.ishland.c2me.opts.accel.opencl.mixin.access.ICheckedRandomSplitter;
import com.ishland.c2me.opts.accel.opencl.mixin.access.IChunkNoiseSampler;
import com.ishland.c2me.opts.accel.opencl.mixin.access.IXoroshiro128PlusPlusRandomSplitter;
import com.ishland.c2me.opts.accel.opencl.common.compiler.emitters.misc.CLBlockStateMappings;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.function.Function;
import com.ishland.c2me.opts.accel.opencl.common.util.StaticCache2D;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.Beardifier;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.Aquifer.FluidPicker;
import net.minecraft.world.level.levelgen.Aquifer.FluidStatus;
import net.minecraft.world.level.levelgen.Beardifier.Rigid;
import net.minecraft.world.level.levelgen.LegacyRandomSource.LegacyPositionalRandomFactory;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource.XoroshiroPositionalRandomFactory;
import net.minecraft.world.level.levelgen.structure.pools.JigsawJunction;
import org.jetbrains.annotations.NotNull;

/**
 * OpenCL data struct layout helpers - Java 17 port (ByteBuffer instead of java.lang.foreign).
 * All structs are 4-byte aligned sequences of int32 (matching the original MemoryLayout definitions).
 */
public class OpenCLStructs {
   public static final int GLOBAL_OFFSET_TABLE_START = 128;
   public static final int LOCAL_OFFSET_TABLE_START = 64;
   public static final int SIZE_estimateSurfaceHeight = 36;

   // worldgen_params (24 x int32)
   public static final int worldgen_params$startBiomeX = 0;
   public static final int worldgen_params$startBiomeZ = 4;
   public static final int worldgen_params$sizeBiomeX = 8;
   public static final int worldgen_params$sizeBiomeZ = 12;
   public static final int worldgen_params$startCellX = 16;
   public static final int worldgen_params$startCellY = 20;
   public static final int worldgen_params$startCellZ = 24;
   public static final int worldgen_params$sizeCellX = 28;
   public static final int worldgen_params$sizeCellY = 32;
   public static final int worldgen_params$sizeCellZ = 36;
   public static final int worldgen_params$estimateSurfaceHeight_startBiomeX = 40;
   public static final int worldgen_params$estimateSurfaceHeight_startBiomeZ = 44;
   public static final int worldgen_params$estimateSurfaceHeight_sizeBiomeX = 48;
   public static final int worldgen_params$estimateSurfaceHeight_sizeBiomeZ = 52;
   public static final int worldgen_params$cache2d_startX = 56;
   public static final int worldgen_params$cache2d_startZ = 60;
   public static final int worldgen_params$cache2d_sizeX = 64;
   public static final int worldgen_params$cache2d_sizeZ = 68;
   public static final int worldgen_params$offset_estimateSurfaceHeight = 72;
   public static final int worldgen_params$genConfig_defaultBlock = 76;
   public static final int worldgen_params$genConfig_defaultFluid = 80;
   public static final int worldgen_params$offset_aquifer = 84;
   public static final int worldgen_params$offset_fluidLevelSampler = 88;
   public static final int worldgen_params$offset_oreVeinRandom = 92;
   public static final int worldgen_params_SIZE = 96;

   // sws_data (19 x int32)
   public static final int sws_data$pieceLength = 0;
   public static final int sws_data$boxStartX = 4;
   public static final int sws_data$boxStartY = 8;
   public static final int sws_data$boxStartZ = 12;
   public static final int sws_data$boxEndX = 16;
   public static final int sws_data$boxEndY = 20;
   public static final int sws_data$boxEndZ = 24;
   public static final int sws_data$groundLevelDelta = 28;
   public static final int sws_data$terrainAdjustment = 32;
   public static final int sws_data$funcLength = 36;
   public static final int sws_data$sourceX = 40;
   public static final int sws_data$sourceGroundY = 44;
   public static final int sws_data$sourceZ = 48;
   public static final int sws_data$affectedBox_startX = 52;
   public static final int sws_data$affectedBox_startY = 56;
   public static final int sws_data$affectedBox_startZ = 60;
   public static final int sws_data$affectedBox_endX = 64;
   public static final int sws_data$affectedBox_endY = 68;
   public static final int sws_data$affectedBox_endZ = 72;
   public static final int sws_data_SIZE = 76;

   // sws_index (4 x int32)
   public static final int sws_index$startX = 0;
   public static final int sws_index$startZ = 4;
   public static final int sws_index$sizeX = 8;
   public static final int sws_index$sizeZ = 12;
   public static final int sws_index_SIZE = 16;

   // aquifer_data (11 x int32)
   public static final int aquifer_data$startX = 0;
   public static final int aquifer_data$startY = 4;
   public static final int aquifer_data$startZ = 8;
   public static final int aquifer_data$sizeX = 12;
   public static final int aquifer_data$sizeY = 16;
   public static final int aquifer_data$sizeZ = 20;
   public static final int aquifer_data$samplingYLowPassCutoff = 24;
   public static final int aquifer_data$randomDeriver = 28;
   public static final int aquifer_data$posIdx_len = 32;
   public static final int aquifer_data$waterLevels = 36;
   public static final int aquifer_data$packedBlockPositions = 40;
   public static final int aquifer_data_SIZE = 44;

   private static Object reflectField(Object o, String name) {
      try {
         java.lang.reflect.Field f = o.getClass().getDeclaredField(name);
         f.setAccessible(true);
         return f.get(o);
      } catch (Throwable t) {
         throw new IllegalStateException("Cannot reflect field " + name + " on " + o.getClass().getName(), t);
      }
   }

   private static ByteBuffer alloc(long size, long alignment) {
      long alignedSize = com.ishland.c2me.opts.accel.opencl.common.util.MemoryUtil.roundUp(size, alignment);
      return org.lwjgl.system.MemoryUtil.memAlignedAlloc((int) alignment, (int) alignedSize);
   }

   public static void putInt(ByteBuffer data, int offset, int value) {
      data.putInt(offset, value);
   }

   private static void putLong(ByteBuffer data, int offset, long value) {
      data.putLong(offset, value);
   }

   private static void copy(ByteBuffer src, int srcOffset, ByteBuffer dst, int dstOffset, int length) {
      for (int i = 0; i < length; i++) {
         dst.put(dstOffset + i, src.get(srcOffset + i));
      }
   }

   public static ByteBuffer sws_data$create(Beardifier sampler) {
      ObjectListIterator<Rigid> piecesIter = ((com.ishland.c2me.opts.accel.opencl.mixin.access.IBeardifierAccess) sampler).c2me$getPieceIterator();
      ObjectListIterator<JigsawJunction> junctionsIter = ((com.ishland.c2me.opts.accel.opencl.mixin.access.IBeardifierAccess) sampler).c2me$getJunctionIterator();
      Rigid[] pieceArray = Iterators.toArray(piecesIter, Rigid.class);
      JigsawJunction[] junctionArray = Iterators.toArray(junctionsIter, JigsawJunction.class);
      piecesIter.back(Integer.MAX_VALUE);
      junctionsIter.back(Integer.MAX_VALUE);
      int offset_boxStartX = com.ishland.c2me.opts.accel.opencl.common.util.MemoryUtil.roundUp(sws_data_SIZE, 4);
      int offset_boxStartY = com.ishland.c2me.opts.accel.opencl.common.util.MemoryUtil.roundUp(offset_boxStartX + pieceArray.length * 4, 4);
      int offset_boxStartZ = com.ishland.c2me.opts.accel.opencl.common.util.MemoryUtil.roundUp(offset_boxStartY + pieceArray.length * 4, 4);
      int offset_boxEndX = com.ishland.c2me.opts.accel.opencl.common.util.MemoryUtil.roundUp(offset_boxStartZ + pieceArray.length * 4, 4);
      int offset_boxEndY = com.ishland.c2me.opts.accel.opencl.common.util.MemoryUtil.roundUp(offset_boxEndX + pieceArray.length * 4, 4);
      int offset_boxEndZ = com.ishland.c2me.opts.accel.opencl.common.util.MemoryUtil.roundUp(offset_boxEndY + pieceArray.length * 4, 4);
      int offset_groundLevelDelta = com.ishland.c2me.opts.accel.opencl.common.util.MemoryUtil.roundUp(offset_boxEndZ + pieceArray.length * 4, 4);
      int offset_terrainAdjustment = com.ishland.c2me.opts.accel.opencl.common.util.MemoryUtil.roundUp(offset_groundLevelDelta + pieceArray.length * 4, 4);
      int offset_sourceX = com.ishland.c2me.opts.accel.opencl.common.util.MemoryUtil.roundUp(offset_terrainAdjustment + pieceArray.length * 4, 4);
      int offset_sourceGroundY = com.ishland.c2me.opts.accel.opencl.common.util.MemoryUtil.roundUp(offset_sourceX + junctionArray.length * 4, 4);
      int offset_sourceZ = com.ishland.c2me.opts.accel.opencl.common.util.MemoryUtil.roundUp(offset_sourceGroundY + junctionArray.length * 4, 4);
      ByteBuffer data = alloc(com.ishland.c2me.opts.accel.opencl.common.util.MemoryUtil.roundUp(offset_sourceZ + junctionArray.length * 4, 4), 64);
      putInt(data, sws_data$pieceLength, pieceArray.length);
      putInt(data, sws_data$boxStartX, offset_boxStartX);
      putInt(data, sws_data$boxStartY, offset_boxStartY);
      putInt(data, sws_data$boxStartZ, offset_boxStartZ);
      putInt(data, sws_data$boxEndX, offset_boxEndX);
      putInt(data, sws_data$boxEndY, offset_boxEndY);
      putInt(data, sws_data$boxEndZ, offset_boxEndZ);
      putInt(data, sws_data$groundLevelDelta, offset_groundLevelDelta);
      putInt(data, sws_data$terrainAdjustment, offset_terrainAdjustment);
      putInt(data, sws_data$funcLength, junctionArray.length);
      putInt(data, sws_data$sourceX, offset_sourceX);
      putInt(data, sws_data$sourceGroundY, offset_sourceGroundY);
      putInt(data, sws_data$sourceZ, offset_sourceZ);
      putInt(data, sws_data$affectedBox_startX, Integer.MIN_VALUE);
      putInt(data, sws_data$affectedBox_startY, Integer.MIN_VALUE);
      putInt(data, sws_data$affectedBox_startZ, Integer.MIN_VALUE);
      putInt(data, sws_data$affectedBox_endX, Integer.MAX_VALUE);
      putInt(data, sws_data$affectedBox_endY, Integer.MAX_VALUE);
      putInt(data, sws_data$affectedBox_endZ, Integer.MAX_VALUE);

      for (int i = 0; i < pieceArray.length; i++) {
         putInt(data, offset_boxStartX + i * 4, pieceArray[i].box().minX());
         putInt(data, offset_boxStartY + i * 4, pieceArray[i].box().minY());
         putInt(data, offset_boxStartZ + i * 4, pieceArray[i].box().minZ());
         putInt(data, offset_boxEndX + i * 4, pieceArray[i].box().maxX());
         putInt(data, offset_boxEndY + i * 4, pieceArray[i].box().maxY());
         putInt(data, offset_boxEndZ + i * 4, pieceArray[i].box().maxZ());
         putInt(data, offset_groundLevelDelta + i * 4, pieceArray[i].groundLevelDelta());
         putInt(data, offset_terrainAdjustment + i * 4, switch (pieceArray[i].terrainAdjustment()) {
            case NONE -> 0;
            case BURY -> 1;
            case BEARD_THIN -> 2;
            case BEARD_BOX -> 3;
            default -> throw new IllegalStateException();
         });
      }

      for (int i = 0; i < junctionArray.length; i++) {
         putInt(data, offset_sourceX + i * 4, junctionArray[i].getSourceX());
         putInt(data, offset_sourceGroundY + i * 4, junctionArray[i].getSourceGroundY());
         putInt(data, offset_sourceZ + i * 4, junctionArray[i].getSourceZ());
      }

      return data;
   }

   public static ByteBuffer sws_index$createForSingleChunk(ChunkPos pos, Beardifier sampler) {
      ByteBuffer memorySegment = sws_data$create(sampler);
      ByteBuffer data = alloc(sws_index_SIZE + 4L + memorySegment.capacity(), 4);
      putInt(data, sws_index$startX, pos.x);
      putInt(data, sws_index$startZ, pos.z);
      putInt(data, sws_index$sizeX, 1);
      putInt(data, sws_index$sizeZ, 1);
      putInt(data, sws_index_SIZE, sws_index_SIZE + 4);
      copy(memorySegment, 0, data, sws_index_SIZE + 4, memorySegment.capacity());
      return data;
   }

   public static ByteBuffer sws_index$createForArea(
      ChunkPos pos, StaticCache2D<ProtoChunk> regionArray, Function<ProtoChunk, NoiseChunk> toChunkNoiseSampler, int sizeX, int sizeZ
   ) {
      ByteBuffer data = alloc(com.ishland.c2me.opts.accel.opencl.common.util.MemoryUtil.roundUp((long) sws_index_SIZE * (sizeX * sizeZ) + 4L, 512L), 4);
      putInt(data, sws_index$startX, pos.x);
      putInt(data, sws_index$startZ, pos.z);
      putInt(data, sws_index$sizeX, sizeX);
      putInt(data, sws_index$sizeZ, sizeZ);
      int currentOffset = sws_index_SIZE + 4 * sizeX * sizeZ;
      putInt(data, sws_index_SIZE, currentOffset);

      for (int dx = 0; dx < sizeX; dx++) {
         for (int dz = 0; dz < sizeZ; dz++) {
            ProtoChunk chunk = regionArray.get(pos.x + dx, pos.z + dz);
            NoiseChunk sampler = toChunkNoiseSampler.apply(chunk);
            Beardifier structureWeightSampler = (Beardifier) ((IChunkNoiseSampler) sampler).getBeardifying();
            if (structureWeightSampler == null) {
               putInt(data, sws_index_SIZE + 4 * (dx * sizeZ + dz), 0);
            } else {
               ByteBuffer memorySegment = sws_data$create(structureWeightSampler);
               currentOffset = com.ishland.c2me.opts.accel.opencl.common.util.MemoryUtil.roundUp(currentOffset, 8);
               putInt(data, sws_index_SIZE + 4 * (dx * sizeZ + dz), currentOffset);

               while (data.capacity() < (long) currentOffset + memorySegment.capacity()) {
                  ByteBuffer newData = alloc(data.capacity() * 2L, 4);
                  copy(data, 0, newData, 0, data.capacity());
                  data = newData;
               }

               copy(memorySegment, 0, data, currentOffset, memorySegment.capacity());
               currentOffset += memorySegment.capacity();
            }
         }
      }

      ByteBuffer sliced = alloc(com.ishland.c2me.opts.accel.opencl.common.util.MemoryUtil.roundUp(currentOffset, 8), 4);
      copy(data, 0, sliced, 0, com.ishland.c2me.opts.accel.opencl.common.util.MemoryUtil.roundUp(currentOffset, 8));
      return sliced;
   }

   public static void setRandomState(ByteBuffer data, PositionalRandomFactory randomSplitter) {
      Objects.requireNonNull(randomSplitter);
      if (randomSplitter instanceof LegacyPositionalRandomFactory s) {
         putLong(data, 0, 0L);
         putLong(data, 8, ((ICheckedRandomSplitter) s).getSeed());
         putLong(data, 16, 0L);
      } else if (randomSplitter instanceof XoroshiroPositionalRandomFactory sx) {
         putLong(data, 0, 1L);
         putLong(data, 8, ((IXoroshiro128PlusPlusRandomSplitter) sx).getSeedLo());
         putLong(data, 16, ((IXoroshiro128PlusPlusRandomSplitter) sx).getSeedHi());
      } else {
         throw new UnsupportedOperationException("Unknown random splitter type " + randomSplitter.getClass().getName());
      }
   }

   @NotNull
   public static ByteBuffer aquifer_data$create(
      int startX, int startY, int startZ, int sizeX, int sizeY, int sizeZ, int samplingYLowPassCutoff, PositionalRandomFactory randomDeriver1
   ) {
      int cacheLength = sizeX * sizeY * sizeZ;
      int offset_randomDeriver = com.ishland.c2me.opts.accel.opencl.common.util.MemoryUtil.roundUp(aquifer_data_SIZE, 8);
      int offset_waterLevels = com.ishland.c2me.opts.accel.opencl.common.util.MemoryUtil.roundUp(offset_randomDeriver + 24, 4);
      int offset_packedBlockPositions = com.ishland.c2me.opts.accel.opencl.common.util.MemoryUtil.roundUp(offset_waterLevels + cacheLength * 8, 4);
      ByteBuffer data = alloc(com.ishland.c2me.opts.accel.opencl.common.util.MemoryUtil.roundUp(offset_packedBlockPositions + cacheLength * 2, 4), 64);
      ByteBuffer randomDeriver = data.duplicate();
      randomDeriver.position(offset_randomDeriver).limit(offset_randomDeriver + 24);
      ByteBuffer randomDeriverSlice = randomDeriver.slice();
      putInt(data, aquifer_data$startX, startX);
      putInt(data, aquifer_data$startY, startY);
      putInt(data, aquifer_data$startZ, startZ);
      putInt(data, aquifer_data$samplingYLowPassCutoff, samplingYLowPassCutoff);
      putInt(data, aquifer_data$sizeX, sizeX);
      putInt(data, aquifer_data$sizeY, sizeY);
      putInt(data, aquifer_data$sizeZ, sizeZ);
      putInt(data, aquifer_data$randomDeriver, offset_randomDeriver);
      putInt(data, aquifer_data$posIdx_len, cacheLength);
      putInt(data, aquifer_data$waterLevels, offset_waterLevels);
      putInt(data, aquifer_data$packedBlockPositions, offset_packedBlockPositions);
      setRandomState(randomDeriverSlice, randomDeriver1);
      return data;
   }

   public static ByteBuffer fluidLevelSamplerCreate(int minimumY, int height, FluidPicker fluidLevelSampler, CLBlockStateMappings mappings) {
      ByteBuffer data = alloc((long) (height + 1) * 8L, 4);
      for (int i = 0; i <= height; i++) {
         int y = i + minimumY;
         FluidStatus fluidLevel = fluidLevelSampler.computeFluid(0, y, 0);
         putInt(data, i * 8, ((com.ishland.c2me.opts.accel.opencl.mixin.access.IFluidStatusAccess) (Object) fluidLevel).c2me$getFluidLevel());
         putInt(data, i * 8 + 4, mappings.toId(((com.ishland.c2me.opts.accel.opencl.mixin.access.IFluidStatusAccess) (Object) fluidLevel).c2me$getFluidType()));
      }
      return data;
   }
}
