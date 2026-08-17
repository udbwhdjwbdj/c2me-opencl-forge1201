package com.ishland.c2me.opts.dfc.common.ast.opto.passes;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.AstTransformer;
import com.ishland.c2me.opts.dfc.common.ast.misc.CacheLikeNode;
import net.minecraft.world.level.levelgen.DensityFunctions.MarkerOrMarked;
import net.minecraft.world.level.levelgen.DensityFunctions.Marker.Type;

public class CacheElimination implements AstTransformer {
   public static final CacheElimination INSTANCE = new CacheElimination();

   private CacheElimination() {
   }

   @Override
   public AstNode transform(AstNode astNode) {
      if (astNode instanceof CacheLikeNode cacheLikeNode && cacheLikeNode.getCacheLike() instanceof MarkerOrMarked wrapping && wrapping.type() == Type.FlatCache) {
         AstNode transformed = cacheLikeNode.getDelegate().transform(CacheElimination.CacheLikeStripper.INSTANCE);
         if (transformed != cacheLikeNode.getDelegate()) {
            return new CacheLikeNode(cacheLikeNode.getCacheLike(), transformed);
         }
      }

      return astNode;
   }

   private static class CacheLikeStripper implements AstTransformer {
      private static CacheElimination.CacheLikeStripper INSTANCE = new CacheElimination.CacheLikeStripper();

      @Override
      public AstNode transform(AstNode astNode) {
         return astNode instanceof CacheLikeNode cacheLikeNode ? cacheLikeNode.getDelegate() : astNode;
      }
   }
}
