package com.ishland.c2me.opts.dfc.common.ast.unary;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;

public class SqueezeNode extends AbstractUnaryNode {
   public SqueezeNode(AstNode operand) {
      super(operand);
   }

   @Override
   protected AstNode newInstance(AstNode operand) {
      return new SqueezeNode(operand);
   }
}
