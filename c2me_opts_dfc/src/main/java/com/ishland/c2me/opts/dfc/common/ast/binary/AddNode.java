package com.ishland.c2me.opts.dfc.common.ast.binary;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;

public class AddNode extends AbstractBinaryNode {
   public AddNode(AstNode left, AstNode right) {
      super(left, right);
   }

   @Override
   protected AstNode newInstance(AstNode left, AstNode right) {
      return new AddNode(left, right);
   }
}
