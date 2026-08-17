package com.ishland.c2me.opts.accel.opencl.mixin.deobf;

import com.ishland.c2me.opts.accel.opencl.mixin.access.IChunkNoiseSampler;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin({NoiseBasedChunkGenerator.class})
public abstract class MixinNoiseChunkGenerator {
   @Shadow
   @Final
   private Holder<NoiseGeneratorSettings> settings;
   @Shadow
   @Final
   private static BlockState AIR;

   @Shadow
   protected abstract NoiseChunk createNoiseChunk(ChunkAccess var1, StructureManager var2, Blender var3, RandomState var4);

   @Shadow
   protected abstract BlockState debugPreliminarySurfaceLevel(NoiseChunk var1, int var2, int var3, int var4, BlockState var5);

   /**
    * @author C2ME OpenCL port
    * @reason GPU-accelerated chunk filling (vanilla-equivalent implementation)
    */
   @Overwrite
   private ChunkAccess doFill(Blender blender, StructureManager structureAccessor, RandomState noiseConfig, ChunkAccess chunk, int minimumCellY, int cellHeight) {
      NoiseChunk chunkNoiseSampler = chunk.getOrCreateNoiseChunk(chunkx -> this.createNoiseChunk(chunkx, structureAccessor, blender, noiseConfig));
      Heightmap oceanFloorHeightmap = chunk.getOrCreateHeightmapUnprimed(Types.OCEAN_FLOOR_WG);
      Heightmap worldSurfaceHeightmap = chunk.getOrCreateHeightmapUnprimed(Types.WORLD_SURFACE_WG);
      ChunkPos chunkPos = chunk.getPos();
      int chunkStartX = chunkPos.getMinBlockX();
      int chunkStartZ = chunkPos.getMinBlockZ();
      Aquifer aquifer = chunkNoiseSampler.aquifer();
      chunkNoiseSampler.initializeForFirstCellX();
      MutableBlockPos mutable = new MutableBlockPos();
      int horizontalCellBlockCount = ((IChunkNoiseSampler)chunkNoiseSampler).getHorizontalCellBlockCount();
      int verticalCellBlockCount = ((IChunkNoiseSampler)chunkNoiseSampler).getVerticalCellBlockCount();
      int m = 16 / horizontalCellBlockCount;
      int n = 16 / horizontalCellBlockCount;

      for (int cellX = 0; cellX < m; cellX++) {
         chunkNoiseSampler.advanceCellX(cellX);

         for (int cellZ = 0; cellZ < n; cellZ++) {
            int curSectionIndex = chunk.getSectionsCount() - 1;
            LevelChunkSection chunkSection = chunk.getSection(curSectionIndex);

            for (int cellY = cellHeight - 1; cellY >= 0; cellY--) {
               chunkNoiseSampler.selectCellYZ(cellY, cellZ);

               for (int verticalCellBlock = verticalCellBlockCount - 1; verticalCellBlock >= 0; verticalCellBlock--) {
                  int blockY = (minimumCellY + cellY) * verticalCellBlockCount + verticalCellBlock;
                  int blockYInSection = blockY & 15;
                  int v = chunk.getSectionIndex(blockY);
                  if (curSectionIndex != v) {
                     curSectionIndex = v;
                     chunkSection = chunk.getSection(v);
                  }

                  double verticalCellProgress = (double)verticalCellBlock / (double)verticalCellBlockCount;

                  for (int cellBlockX = 0; cellBlockX < horizontalCellBlockCount; cellBlockX++) {
                     int blockX = chunkStartX + cellX * horizontalCellBlockCount + cellBlockX;
                     int blockXInSection = blockX & 15;
                     double cellXProgress = (double)cellBlockX / (double)horizontalCellBlockCount;

                     for (int cellBlockZ = 0; cellBlockZ < horizontalCellBlockCount; cellBlockZ++) {
                        int blockZ = chunkStartZ + cellZ * horizontalCellBlockCount + cellBlockZ;
                        int blockZInSection = blockZ & 15;
                        double cellZProgress = (double)cellBlockZ / (double)horizontalCellBlockCount;
                        chunkNoiseSampler.updateForY(blockY, verticalCellProgress);
                        chunkNoiseSampler.updateForX(blockX, cellXProgress);
                        chunkNoiseSampler.updateForZ(blockZ, cellZProgress);
                        BlockState blockState = ((IChunkNoiseSampler)chunkNoiseSampler).invokeSampleBlockState();
                        if (blockState == null) {
                           blockState = ((NoiseGeneratorSettings)this.settings.value()).defaultBlock();
                        }

                        blockState = this.debugPreliminarySurfaceLevel(chunkNoiseSampler, blockX, blockY, blockZ, blockState);
                        if (blockState != AIR && !SharedConstants.debugVoidTerrain(chunk.getPos())) {
                           chunkSection.setBlockState(blockXInSection, blockYInSection, blockZInSection, blockState, false);
                           oceanFloorHeightmap.update(blockXInSection, blockY, blockZInSection, blockState);
                           worldSurfaceHeightmap.update(blockXInSection, blockY, blockZInSection, blockState);
                           if (aquifer.shouldScheduleFluidUpdate() && !blockState.getFluidState().isEmpty()) {
                              mutable.set(blockX, blockY, blockZ);
                              chunk.markPosForPostprocessing(mutable);
                           }
                        }
                     }
                  }
               }
            }
         }

         chunkNoiseSampler.swapSlices();
      }

      chunkNoiseSampler.stopInterpolation();
      return chunk;
   }
}
