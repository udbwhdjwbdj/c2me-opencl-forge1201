package com.ishland.c2me.opts.dfc.common.ast.opto.passes;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.AstTransformer;
import com.ishland.c2me.opts.dfc.common.ast.misc.ConstantNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.RangeChoiceNode;
import java.util.Objects;

public class BranchElimination implements AstTransformer {
   public static final BranchElimination INSTANCE = new BranchElimination();

   private BranchElimination() {
   }

   @Override
   public AstNode transform(AstNode astNode) {
      Objects.requireNonNull(astNode);

      if (astNode instanceof RangeChoiceNode rangeChoiceNode) {
         if (rangeChoiceNode.input instanceof ConstantNode c) {
            return c.getValue() >= rangeChoiceNode.minInclusive && c.getValue() < rangeChoiceNode.maxExclusive
               ? rangeChoiceNode.whenInRange
               : rangeChoiceNode.whenOutOfRange;
         }
         return rangeChoiceNode.whenInRange.equals(rangeChoiceNode.whenOutOfRange) ? rangeChoiceNode.whenInRange : rangeChoiceNode;
      }
      return astNode;
   }
}
