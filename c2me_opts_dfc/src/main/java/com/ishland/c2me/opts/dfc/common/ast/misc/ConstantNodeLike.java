package com.ishland.c2me.opts.dfc.common.ast.misc;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.gen.meta.ValuesMethodDef;

public interface ConstantNodeLike extends AstNode {
   ValuesMethodDef getDef();
}
