package com.ishland.c2me.opts.dfc.common.ast.binary;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.AstTransformer;
import java.util.Objects;

public abstract class AbstractBinaryNode implements AstNode {
   public final AstNode left;
   public final AstNode right;

   public AbstractBinaryNode(AstNode left, AstNode right) {
      this.left = Objects.requireNonNull(left);
      this.right = Objects.requireNonNull(right);
   }

   @Override
   public AstNode[] getChildren() {
      return new AstNode[]{this.left, this.right};
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         AbstractBinaryNode that = (AbstractBinaryNode)o;
         return Objects.equals(this.left, that.left) && Objects.equals(this.right, that.right);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      int result = 1;
      result = 31 * result + this.getClass().hashCode();
      result = 31 * result + this.left.hashCode();
      return 31 * result + this.right.hashCode();
   }

   @Override
   public boolean relaxedEquals(AstNode o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         AbstractBinaryNode that = (AbstractBinaryNode)o;
         return this.left.relaxedEquals(that.left) && this.right.relaxedEquals(that.right);
      } else {
         return false;
      }
   }

   @Override
   public int relaxedHashCode() {
      int result = 1;
      result = 31 * result + this.getClass().hashCode();
      result = 31 * result + this.left.relaxedHashCode();
      return 31 * result + this.right.relaxedHashCode();
   }

   protected abstract AstNode newInstance(AstNode var1, AstNode var2);

   @Override
   public AstNode transform(AstTransformer transformer) {
      AstNode left = this.left.transform(transformer);
      AstNode right = this.right.transform(transformer);
      return left == this.left && right == this.right ? transformer.transform(this) : transformer.transform(this.newInstance(left, right));
   }

   public AstNode swapOperands() {
      return this.newInstance(this.right, this.left);
   }

   public boolean canSwapOperandsSafely() {
      return true;
   }
}
