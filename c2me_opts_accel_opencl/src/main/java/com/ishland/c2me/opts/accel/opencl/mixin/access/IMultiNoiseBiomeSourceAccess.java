package com.ishland.c2me.opts.accel.opencl.mixin.access;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate.ParameterList;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MultiNoiseBiomeSource.class)
public interface IMultiNoiseBiomeSourceAccess {
   @Invoker("parameters")
   ParameterList<Holder<Biome>> c2me$parameters();
}
