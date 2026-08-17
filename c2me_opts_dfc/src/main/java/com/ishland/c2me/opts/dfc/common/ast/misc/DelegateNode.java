package com.ishland.c2me.opts.dfc.common.ast.misc;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.AstTransformer;
import java.util.Objects;
import net.minecraft.world.level.levelgen.DensityFunction;

public class DelegateNode implements AstNode {
   private final DensityFunction densityFunction;

   public DelegateNode(DensityFunction densityFunction) {
      this.densityFunction = Objects.requireNonNull(densityFunction);
   }

   @Override
   public AstNode[] getChildren() {
      return new AstNode[0];
   }

   @Override
   public AstNode transform(AstTransformer transformer) {
      return transformer.transform(this);
   }

   public DensityFunction getDelegate() {
      return this.densityFunction;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         DelegateNode that = (DelegateNode)o;
         return Objects.equals(this.densityFunction, that.densityFunction);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      int result = 1;
      result = 31 * result + Objects.hashCode(this.getClass());
      return 31 * result + Objects.hashCode(this.densityFunction);
   }

   @Override
   public boolean relaxedEquals(AstNode o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         DelegateNode that = (DelegateNode)o;
         return this.densityFunction.getClass() == that.densityFunction.getClass();
      } else {
         return false;
      }
   }

   @Override
   public int relaxedHashCode() {
      int result = 1;
      result = 31 * result + Objects.hashCode(this.getClass());
      return 31 * result + Objects.hashCode(this.densityFunction.getClass());
   }
}
