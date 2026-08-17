package com.ishland.c2me.opts.dfc.common.ast.binary;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;

public class MaxShortNode extends AbstractBinaryNode {
   public final double rightMax;

   public MaxShortNode(AstNode left, AstNode right, double rightMax) {
      super(left, right);
      this.rightMax = rightMax;
   }

   @Override
   protected AstNode newInstance(AstNode left, AstNode right) {
      return new MaxShortNode(left, right, this.rightMax);
   }

   @Override
   public boolean canSwapOperandsSafely() {
      return false;
   }
}
