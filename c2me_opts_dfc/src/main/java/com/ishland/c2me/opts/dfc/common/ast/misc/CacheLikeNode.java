package com.ishland.c2me.opts.dfc.common.ast.misc;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.AstTransformer;
import com.ishland.c2me.opts.dfc.common.ducks.IFastCacheLike;
import java.util.Objects;
import net.minecraft.world.level.levelgen.DensityFunctions.MarkerOrMarked;

public class CacheLikeNode implements AstNode {
   private final IFastCacheLike cacheLike;
   private final AstNode delegate;

   public CacheLikeNode(IFastCacheLike cacheLike, AstNode delegate) {
      this.cacheLike = cacheLike;
      this.delegate = Objects.requireNonNull(delegate);
   }

   @Override
   public AstNode[] getChildren() {
      return new AstNode[]{this.delegate};
   }

   @Override
   public AstNode transform(AstTransformer transformer) {
      AstNode delegate = this.delegate.transform(transformer);
      return this.delegate == delegate ? transformer.transform(this) : transformer.transform(new CacheLikeNode(this.cacheLike, delegate));
   }

   public IFastCacheLike getCacheLike() {
      return this.cacheLike;
   }

   public AstNode getDelegate() {
      return this.delegate;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         CacheLikeNode that = (CacheLikeNode)o;
         return equals(this.cacheLike, that.cacheLike) && Objects.equals(this.delegate, that.delegate);
      } else {
         return false;
      }
   }

   private static boolean equals(IFastCacheLike a, IFastCacheLike b) {
      if (a instanceof MarkerOrMarked wrappingA && b instanceof MarkerOrMarked wrappingB) {
         return wrappingA.type() == wrappingB.type();
      }

      return a.equals(b);
   }

   @Override
   public int hashCode() {
      int result = 1;
      result = 31 * result + this.getClass().hashCode();
      result = 31 * result + hashCode(this.cacheLike);
      return 31 * result + this.delegate.hashCode();
   }

   private static int hashCode(IFastCacheLike o) {
      return o instanceof MarkerOrMarked wrapping ? wrapping.type().hashCode() : o.hashCode();
   }

   @Override
   public boolean relaxedEquals(AstNode o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         CacheLikeNode that = (CacheLikeNode)o;
         return relaxedEquals(this.cacheLike, that.cacheLike) && this.delegate.relaxedEquals(that.delegate);
      } else {
         return false;
      }
   }

   private static boolean relaxedEquals(IFastCacheLike a, IFastCacheLike b) {
      if (a instanceof MarkerOrMarked wrappingA && b instanceof MarkerOrMarked wrappingB) {
         return wrappingA.type() == wrappingB.type();
      }

      return a.getClass() == b.getClass();
   }

   @Override
   public int relaxedHashCode() {
      int result = 1;
      result = 31 * result + this.getClass().hashCode();
      result = 31 * result + relaxedHashCode(this.cacheLike);
      return 31 * result + this.delegate.relaxedHashCode();
   }

   private static int relaxedHashCode(IFastCacheLike o) {
      return o instanceof MarkerOrMarked wrapping ? wrapping.type().hashCode() : o.getClass().hashCode();
   }
}
