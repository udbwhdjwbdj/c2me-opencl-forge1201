package com.ishland.c2me.opts.accel.opencl.mixin.access;

import net.minecraft.world.level.biome.Climate.Parameter;
import net.minecraft.world.level.biome.Climate.RTree.Node;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Node.class)
public interface IRTreeNodeAccess {
   @Accessor("parameterSpace")
   Parameter[] c2me$getParameterSpace();
}
