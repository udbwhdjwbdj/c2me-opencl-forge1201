package com.ishland.c2me.opts.dfc.common.ast.unary;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;

public class NegMulNode extends AbstractUnaryNode {
   public final double negMul;

   public NegMulNode(AstNode operand, double negMul) {
      super(operand);
      this.negMul = negMul;
   }

   @Override
   protected AstNode newInstance(AstNode operand) {
      return new NegMulNode(operand, this.negMul);
   }
}
