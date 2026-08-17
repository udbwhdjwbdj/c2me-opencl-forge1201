package com.ishland.c2me.opts.dfc.common.ast.binary;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;

public class MinShortNode extends AbstractBinaryNode {
   public final double rightMin;

   public MinShortNode(AstNode left, AstNode right, double rightMin) {
      super(left, right);
      this.rightMin = rightMin;
   }

   @Override
   protected AstNode newInstance(AstNode left, AstNode right) {
      return new MinShortNode(left, right, this.rightMin);
   }

   @Override
   public boolean canSwapOperandsSafely() {
      return false;
   }
}
