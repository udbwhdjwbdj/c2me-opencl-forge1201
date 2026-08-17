package com.ishland.c2me.opts.dfc.common.ast.binary;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;

public class MulNode extends AbstractBinaryNode {
   public MulNode(AstNode left, AstNode right) {
      super(left, right);
   }

   @Override
   protected AstNode newInstance(AstNode left, AstNode right) {
      return new MulNode(left, right);
   }
}
