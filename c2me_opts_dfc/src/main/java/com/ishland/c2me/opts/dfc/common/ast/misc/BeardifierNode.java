package com.ishland.c2me.opts.dfc.common.ast.misc;

import com.ishland.flowsched.util.Assertions;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions.BeardifierMarker;

public class BeardifierNode extends DelegateNode {
   public BeardifierNode(DensityFunction densityFunction) {
      super(densityFunction);
      Assertions.assertTrue(densityFunction == BeardifierMarker.INSTANCE);
   }
}
