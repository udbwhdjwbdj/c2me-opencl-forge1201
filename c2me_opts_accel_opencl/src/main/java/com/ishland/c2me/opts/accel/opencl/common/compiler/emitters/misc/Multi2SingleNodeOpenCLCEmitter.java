package com.ishland.c2me.opts.accel.opencl.common.compiler.emitters.misc;

import com.ishland.c2me.opts.dfc.common.ast.misc.Multi2SingleNode;
import com.ishland.c2me.opts.dfc.common.gen.meta.ValuesMethodDef;
import com.ishland.c2me.opts.dfc.common.gen.opencl.OpenCLCEmitter;
import com.ishland.c2me.opts.dfc.common.gen.opencl.OpenCLCGenFunctionContext;

public class Multi2SingleNodeOpenCLCEmitter implements OpenCLCEmitter<Multi2SingleNode> {
   public static final Multi2SingleNodeOpenCLCEmitter INSTANCE = new Multi2SingleNodeOpenCLCEmitter();

   private Multi2SingleNodeOpenCLCEmitter() {
   }

   public String doCLGen(Multi2SingleNode node, OpenCLCGenFunctionContext context, String storeTo) {
      ValuesMethodDef next = context.newVar(node.next);
      return storeTo + " = " + context.getDelegateVar(next, next.returnType()) + ";\n";
   }
}
