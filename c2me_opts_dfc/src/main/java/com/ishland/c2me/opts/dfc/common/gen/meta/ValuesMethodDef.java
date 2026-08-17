package com.ishland.c2me.opts.dfc.common.gen.meta;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;

public interface ValuesMethodDef {
   String generatedMethod();

   boolean isConst();

   AstNode.ReturnType returnType();
}
