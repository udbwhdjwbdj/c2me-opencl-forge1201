package com.ishland.c2me.opts.dfc.common.gen.dot;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.gen.CodeEmitter;

public interface DotEmitter<T extends AstNode> extends CodeEmitter<T> {
   int doDotGen(T var1, DotGen.Context var2, DotGen.Context.Builder var3);
}
