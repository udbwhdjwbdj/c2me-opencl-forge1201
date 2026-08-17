package com.ishland.c2me.opts.accel.opencl.mixin;

import com.ishland.c2me.opts.accel.opencl.common.ducks.ChunkNoiseSamplerExtension;
import com.ishland.c2me.opts.accel.opencl.common.gen.cache.Stage1Cache;
import com.ishland.c2me.opts.accel.opencl.common.util.TLUtil;
import com.ishland.c2me.opts.accel.opencl.common.util.FlowschedAssertions;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.Aquifer.FluidPicker;
import net.minecraft.world.level.levelgen.DensityFunctions.BeardifierOrMarker;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({NoiseChunk.class})
public class MixinChunkNoiseSampler implements ChunkNoiseSamplerExtension {
   @Shadow
   @Final
   private Long2IntMap preliminarySurfaceLevel;
   @Unique
   private FluidPicker c2me$fluidLevelSampler;
   @Unique
   private NoiseGeneratorSettings c2me$chunkGeneratorSettings;
   @Unique
   private RandomState c2me$noiseConfig;

   @Inject(
      method = {"<init>"},
      at = {@At("RETURN")}
   )
   private void earlyInit(
      int horizontalCellCount,
      RandomState noiseConfig,
      int startBlockX,
      int startBlockZ,
      NoiseSettings generationShapeConfig,
      BeardifierOrMarker beardifying,
      NoiseGeneratorSettings chunkGeneratorSettings,
      FluidPicker fluidLevelSampler,
      Blender blender,
      CallbackInfo ci
   ) {
      if (TLUtil.stage1CachePassing.get() != null) {
         Stage1Cache.AreaCacheEntry areaCacheEntry = (Stage1Cache.AreaCacheEntry)TLUtil.stage1CachePassing.get();
         int chunkX = areaCacheEntry.chunkX();
         int chunkZ = areaCacheEntry.chunkZ();
         int sizeX = areaCacheEntry.sizeX() * 4 + 32;
         int sizeZ = areaCacheEntry.sizeZ() * 4 + 32;
         int[] surfaceHeights = areaCacheEntry.surfaceHeights();
         FlowschedAssertions.assertTrue(sizeX * sizeZ == surfaceHeights.length);

         for (int relX = 0; relX < sizeX; relX++) {
            for (int relZ = 0; relZ < sizeZ; relZ++) {
               int cacheRelX = (chunkX - 4 << 2) + relX;
               int cacheRelZ = (chunkZ - 4 << 2) + relZ;
               this.preliminarySurfaceLevel
                  .put(ColumnPos.asLong(QuartPos.toBlock(cacheRelX), QuartPos.toBlock(cacheRelZ)), surfaceHeights[relX * sizeZ + relZ]);
            }
         }
      }
   }

   @Inject(
      method = {"<init>"},
      at = {@At("RETURN")}
   )
   private void onInit(
      int horizontalCellCount,
      RandomState noiseConfig,
      int startBlockX,
      int startBlockZ,
      NoiseSettings generationShapeConfig,
      BeardifierOrMarker beardifying,
      NoiseGeneratorSettings chunkGeneratorSettings,
      FluidPicker fluidLevelSampler,
      Blender blender,
      CallbackInfo ci
   ) {
      this.c2me$fluidLevelSampler = fluidLevelSampler;
      this.c2me$chunkGeneratorSettings = chunkGeneratorSettings;
      this.c2me$noiseConfig = noiseConfig;
   }

   @Override
   public FluidPicker c2me$getFluidLevelSampler() {
      return this.c2me$fluidLevelSampler;
   }

   @Override
   public NoiseGeneratorSettings c2me$getChunkGeneratorSettings() {
      return this.c2me$chunkGeneratorSettings;
   }

   @Override
   public RandomState c2me$getNoiseConfig() {
      return this.c2me$noiseConfig;
   }
}
