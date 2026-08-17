package com.ishland.c2me.opts.dfc.common.ast.unary;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.AstTransformer;
import java.util.Objects;

public abstract class AbstractUnaryNode implements AstNode {
   public final AstNode operand;

   public AbstractUnaryNode(AstNode operand) {
      this.operand = Objects.requireNonNull(operand);
   }

   @Override
   public AstNode[] getChildren() {
      return new AstNode[]{this.operand};
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         AbstractUnaryNode that = (AbstractUnaryNode)o;
         return Objects.equals(this.operand, that.operand);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      int result = 1;
      result = 31 * result + this.getClass().hashCode();
      return 31 * result + this.operand.hashCode();
   }

   @Override
   public boolean relaxedEquals(AstNode o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         AbstractUnaryNode that = (AbstractUnaryNode)o;
         return this.operand.relaxedEquals(that.operand);
      } else {
         return false;
      }
   }

   @Override
   public int relaxedHashCode() {
      int result = 1;
      result = 31 * result + this.getClass().hashCode();
      return 31 * result + this.operand.relaxedHashCode();
   }

   protected abstract AstNode newInstance(AstNode var1);

   @Override
   public AstNode transform(AstTransformer transformer) {
      AstNode operand = this.operand.transform(transformer);
      return this.operand == operand ? transformer.transform(this) : transformer.transform(this.newInstance(operand));
   }
}
