package com.ishland.c2me.opts.dfc.common.ast.integration.lithostitched.misc;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.AstTransformer;
import java.util.Objects;

public class MixNode implements AstNode {
   public final AstNode input;
   public final AstNode argument1;
   public final AstNode argument2;

   public MixNode(AstNode input, AstNode argument1, AstNode argument2) {
      this.input = Objects.requireNonNull(input);
      this.argument1 = Objects.requireNonNull(argument1);
      this.argument2 = Objects.requireNonNull(argument2);
   }

   @Override
   public AstNode[] getChildren() {
      return new AstNode[]{this.input, this.argument1, this.argument2};
   }

   @Override
   public AstNode transform(AstTransformer transformer) {
      AstNode input = this.input.transform(transformer);
      AstNode argument1 = this.argument1.transform(transformer);
      AstNode argument2 = this.argument2.transform(transformer);
      return this.input == input && this.argument1 == argument1 && this.argument2 == argument2
         ? transformer.transform(this)
         : transformer.transform(new MixNode(input, argument1, argument2));
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         MixNode that = (MixNode)o;
         return Objects.equals(this.input, that.input) && Objects.equals(this.argument1, that.argument1) && Objects.equals(this.argument2, that.argument2);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      int result = 1;
      result = 31 * result + this.getClass().hashCode();
      result = 31 * result + this.input.hashCode();
      result = 31 * result + this.argument1.hashCode();
      return 31 * result + this.argument2.hashCode();
   }

   @Override
   public boolean relaxedEquals(AstNode o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         MixNode that = (MixNode)o;
         return this.input.relaxedEquals(that.input) && this.argument1.relaxedEquals(that.argument1) && this.argument2.relaxedEquals(that.argument2);
      } else {
         return false;
      }
   }

   @Override
   public int relaxedHashCode() {
      int result = 1;
      result = 31 * result + this.getClass().hashCode();
      result = 31 * result + this.input.relaxedHashCode();
      result = 31 * result + this.argument1.relaxedHashCode();
      return 31 * result + this.argument2.relaxedHashCode();
   }
}
