package com.ishland.c2me.opts.accel.opencl.mixin.access;

import net.minecraft.world.level.levelgen.XoroshiroRandomSource.XoroshiroPositionalRandomFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({XoroshiroPositionalRandomFactory.class})
public interface IXoroshiro128PlusPlusRandomSplitter {
   @Accessor("seedLo")
   long getSeedLo();

   @Accessor("seedHi")
   long getSeedHi();
}
