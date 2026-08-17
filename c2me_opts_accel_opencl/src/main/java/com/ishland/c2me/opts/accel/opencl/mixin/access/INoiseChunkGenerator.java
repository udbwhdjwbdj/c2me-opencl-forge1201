package com.ishland.c2me.opts.accel.opencl.mixin.access;

import java.util.function.Supplier;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.Aquifer.FluidPicker;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({NoiseBasedChunkGenerator.class})
public interface INoiseChunkGenerator {
   @Invoker("createNoiseChunk")
   NoiseChunk invokeCreateChunkNoiseSampler(ChunkAccess var1, StructureManager var2, Blender var3, RandomState var4);

   @Accessor("globalFluidPicker")
   Supplier<FluidPicker> getFluidLevelSampler();
}
