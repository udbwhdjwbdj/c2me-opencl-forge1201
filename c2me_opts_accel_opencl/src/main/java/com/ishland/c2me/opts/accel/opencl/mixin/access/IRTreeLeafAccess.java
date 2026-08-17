package com.ishland.c2me.opts.accel.opencl.mixin.access;

import net.minecraft.world.level.biome.Climate.RTree.Leaf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Leaf.class)
public interface IRTreeLeafAccess {
   @Accessor("value")
   Object c2me$getValue();
}
