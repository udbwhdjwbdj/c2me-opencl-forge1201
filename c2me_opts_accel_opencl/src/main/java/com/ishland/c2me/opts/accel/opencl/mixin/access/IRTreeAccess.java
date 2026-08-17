package com.ishland.c2me.opts.accel.opencl.mixin.access;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate.RTree;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RTree.class)
public interface IRTreeAccess {
   @Accessor("root")
   RTree.Node<Holder<Biome>> c2me$getRoot();
}
