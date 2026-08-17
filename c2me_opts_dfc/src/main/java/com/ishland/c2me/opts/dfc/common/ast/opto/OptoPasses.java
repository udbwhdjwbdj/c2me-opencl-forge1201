package com.ishland.c2me.opts.dfc.common.ast.opto;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.AstTransformer;
import com.ishland.c2me.opts.dfc.common.ast.opto.passes.BranchElimination;
import com.ishland.c2me.opts.dfc.common.ast.opto.passes.CacheElimination;
import com.ishland.c2me.opts.dfc.common.ast.opto.passes.FoldConstants;
import com.ishland.c2me.opts.dfc.common.ast.opto.passes.TreeNormalization;

public class OptoPasses {
   private static final AstTransformer[] PASSES = new AstTransformer[]{TreeNormalization.INSTANCE, FoldConstants.INSTANCE, BranchElimination.INSTANCE};
   private static final AstTransformer[] PASSES_OCL = new AstTransformer[]{
      CacheElimination.INSTANCE, TreeNormalization.INSTANCE, FoldConstants.INSTANCE, BranchElimination.INSTANCE
   };

   public static OptoPasses.AstPair optimize(AstNode astNode) {
      return optimize0(astNode, PASSES);
   }

   public static OptoPasses.AstPair optimizeOCL(AstNode astNode) {
      return optimize0(astNode, PASSES_OCL);
   }

   
   private static OptoPasses.AstPair optimize0(AstNode astNode, AstTransformer[] passes) {
      AstNode res = astNode;

      do {
         astNode = res;

         for (AstTransformer pass : passes) {
            res = res.transform(pass);
         }
      } while (res != astNode);

      return new OptoPasses.AstPair(astNode, res);
   }

   public static record AstPair(AstNode unoptimized,  AstNode optimized) {
      public static OptoPasses.AstPair ofOptimizedOnly(AstNode optimized) {
         return new OptoPasses.AstPair(null, optimized);
      }

      public AstNode tryUnoptimized() {
         return this.unoptimized != null ? this.unoptimized : this.optimized;
      }
   }
}
