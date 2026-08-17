package com.ishland.c2me.opts.accel.opencl.common.compiler.emitters.misc;

import com.ishland.c2me.opts.dfc.common.ast.misc.RootNode;
import com.ishland.c2me.opts.dfc.common.gen.meta.ValuesMethodDefF64;
import com.ishland.c2me.opts.dfc.common.gen.opencl.OpenCLCEmitter;
import com.ishland.c2me.opts.dfc.common.gen.opencl.OpenCLCGenFunctionContext;

public class RootNodeOpenCLCEmitter implements OpenCLCEmitter<RootNode> {
   public static final RootNodeOpenCLCEmitter INSTANCE = new RootNodeOpenCLCEmitter();

   private RootNodeOpenCLCEmitter() {
   }

   public String doCLGen(RootNode node, OpenCLCGenFunctionContext context, String storeTo) {
      ValuesMethodDefF64 method = context.newVarF64(node.next);
      return storeTo + " = " + context.getDelegateVar(method) + ";\n";
   }
}
