package com.ishland.c2me.opts.accel.opencl.mixin.access;

import net.minecraft.world.level.levelgen.LegacyRandomSource.LegacyPositionalRandomFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({LegacyPositionalRandomFactory.class})
public interface ICheckedRandomSplitter {
   @Accessor("seed")
   long getSeed();
}
