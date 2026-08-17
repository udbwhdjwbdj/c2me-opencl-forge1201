package com.ishland.c2me.opts.accel.opencl.mixin;

import com.ishland.c2me.opts.accel.opencl.common.ducks.PalettedContainerExtension;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainer.Strategy;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin({PalettedContainer.class})
public abstract class MixinPalettedContainer<T> implements PalettedContainerExtension<T> {
   @Shadow
   @Final
   private Strategy strategy;

   @Shadow
   protected abstract void set(int var1, T var2);

   @Override
   public void c2me$setUnsafe(int x, int y, int z, T value) {
      this.set(this.strategy.getIndex(x, y, z), value);
   }
}
