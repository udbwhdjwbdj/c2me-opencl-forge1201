package com.ishland.c2me.opts.accel.opencl.mixin.access;

import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.minecraft.world.level.levelgen.Beardifier;
import net.minecraft.world.level.levelgen.structure.pools.JigsawJunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Beardifier.class)
public interface IBeardifierAccess {
   @Accessor("pieceIterator")
   ObjectListIterator<Beardifier.Rigid> c2me$getPieceIterator();

   @Accessor("junctionIterator")
   ObjectListIterator<JigsawJunction> c2me$getJunctionIterator();

   @Accessor("BEARD_KERNEL")
   float[] c2me$getBeardKernel();
}
