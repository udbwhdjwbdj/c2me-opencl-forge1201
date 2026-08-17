package com.ishland.c2me.opts.dfc.common.ducks;

import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseRouter;

public interface NoiseRouterExtension {
   DensityFunction c2me$getFinalFinalDensity();

   void c2me$setFinalFinalDensity(DensityFunction var1);

   void c2me$setOriginalNoiseRouter(NoiseRouter var1);

   NoiseRouter c2me$getOriginalNoiseRouter();
}
