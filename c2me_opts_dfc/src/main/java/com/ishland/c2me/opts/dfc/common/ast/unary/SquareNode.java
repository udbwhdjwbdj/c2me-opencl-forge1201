package com.ishland.c2me.opts.dfc.common.ast.unary;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;

public class SquareNode extends AbstractUnaryNode {
   public SquareNode(AstNode operand) {
      super(operand);
   }

   @Override
   protected AstNode newInstance(AstNode operand) {
      return new SquareNode(operand);
   }
}
