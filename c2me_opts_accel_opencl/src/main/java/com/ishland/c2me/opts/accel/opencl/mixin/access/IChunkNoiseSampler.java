package com.ishland.c2me.opts.accel.opencl.mixin.access;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.DensityFunctions.BeardifierOrMarker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({NoiseChunk.class})
public interface IChunkNoiseSampler {
   @Accessor("cellStartBlockX")
   int getStartBlockX();

   @Accessor("cellStartBlockY")
   int getStartBlockY();

   @Accessor("cellStartBlockZ")
   int getStartBlockZ();

   @Accessor("cellWidth")
   int getHorizontalCellBlockCount();

   @Accessor("cellHeight")
   int getVerticalCellBlockCount();

   @Accessor("interpolating")
   boolean getIsInInterpolationLoop();

   @Accessor("fillingCell")
   boolean getIsSamplingForCaches();

   @Accessor("firstNoiseX")
   int getStartBiomeX();

   @Accessor("firstNoiseZ")
   int getStartBiomeZ();

   @Accessor("cellCountXZ")
   int getHorizontalCellCount();

   @Accessor("cellCountY")
   int getVerticalCellCount();

   @Accessor("cellNoiseMinY")
   int getMinimumCellY();

   @Accessor("inCellX")
   int getCellBlockX();

   @Accessor("inCellY")
   int getCellBlockY();

   @Accessor("inCellZ")
   int getCellBlockZ();

   @Invoker("getInterpolatedState")
   BlockState invokeSampleBlockState();

   @Accessor("noiseSizeXZ")
   int getHorizontalBiomeEnd();

   @Accessor("beardifier")
   BeardifierOrMarker getBeardifying();
}
