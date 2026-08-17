package com.ishland.c2me.opts.dfc.common.ast.conversion;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.AstTransformer;
import java.util.Objects;

public class ToF64Node implements AstNode {
   public final AstNode next;

   public ToF64Node(AstNode next) {
      this.next = Objects.requireNonNull(next);
   }

   @Override
   public AstNode[] getChildren() {
      return new AstNode[]{this.next};
   }

   @Override
   public AstNode transform(AstTransformer transformer) {
      AstNode next = this.next.transform(transformer);
      return next == this.next ? transformer.transform(this) : transformer.transform(new ToF64Node(next));
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         ToF64Node that = (ToF64Node)o;
         return Objects.equals(this.next, that.next);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      int result = 1;
      result = 31 * result + this.getClass().hashCode();
      return 31 * result + this.next.hashCode();
   }

   @Override
   public boolean relaxedEquals(AstNode o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         ToF64Node that = (ToF64Node)o;
         return this.next.relaxedEquals(that.next);
      } else {
         return false;
      }
   }

   @Override
   public int relaxedHashCode() {
      int result = 1;
      result = 31 * result + this.getClass().hashCode();
      return 31 * result + this.next.relaxedHashCode();
   }
}
