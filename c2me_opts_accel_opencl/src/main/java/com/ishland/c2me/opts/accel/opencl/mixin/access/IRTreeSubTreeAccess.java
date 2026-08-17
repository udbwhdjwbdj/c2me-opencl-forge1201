package com.ishland.c2me.opts.accel.opencl.mixin.access;

import net.minecraft.world.level.biome.Climate.RTree.Node;
import net.minecraft.world.level.biome.Climate.RTree.SubTree;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SubTree.class)
public interface IRTreeSubTreeAccess {
   @Accessor("children")
   Node<?>[] c2me$getChildren();
}
