package com.ishland.c2me.opts.dfc.common.ast.misc;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import java.util.Objects;
import net.minecraft.world.level.levelgen.DensityFunctions.EndIslandDensityFunction;

public class EndIslandsNode extends DelegateNode {
   public final EndIslandDensityFunction endIslands;

   public EndIslandsNode(EndIslandDensityFunction endIslands) {
      super(endIslands);
      this.endIslands = endIslands;
   }

   @Override
   public boolean equals(Object object) {
      if (this == object) {
         return true;
      } else if (object != null && this.getClass() == object.getClass()) {
         EndIslandsNode that = (EndIslandsNode)object;
         return Objects.equals(this.endIslands, that.endIslands);
      } else {
         return false;
      }
   }

   @Override
   public boolean relaxedEquals(AstNode o) {
      return this.equals(o);
   }

   @Override
   public int relaxedHashCode() {
      return this.hashCode();
   }
}
