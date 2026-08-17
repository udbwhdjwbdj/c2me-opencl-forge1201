package com.ishland.c2me.opts.dfc.common.ast.binary;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;

public class MaxNode extends AbstractBinaryNode {
   public MaxNode(AstNode left, AstNode right) {
      super(left, right);
   }

   @Override
   protected AstNode newInstance(AstNode left, AstNode right) {
      return new MaxNode(left, right);
   }

   @Override
   public boolean canSwapOperandsSafely() {
      return false;
   }
}
