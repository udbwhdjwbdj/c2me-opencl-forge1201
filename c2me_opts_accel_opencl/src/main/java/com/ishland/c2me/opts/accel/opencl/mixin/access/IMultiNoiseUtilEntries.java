package com.ishland.c2me.opts.accel.opencl.mixin.access;

import net.minecraft.world.level.biome.Climate.ParameterList;
import net.minecraft.world.level.biome.Climate.RTree;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({ParameterList.class})
public interface IMultiNoiseUtilEntries<T> {
   @Accessor("index")
   RTree<T> getTree();
}
