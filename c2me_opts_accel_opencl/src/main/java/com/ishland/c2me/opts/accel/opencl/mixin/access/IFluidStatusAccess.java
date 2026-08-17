package com.ishland.c2me.opts.accel.opencl.mixin.access;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Aquifer.FluidStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FluidStatus.class)
public interface IFluidStatusAccess {
   @Accessor("fluidLevel")
   int c2me$getFluidLevel();

   @Accessor("fluidType")
   BlockState c2me$getFluidType();
}
