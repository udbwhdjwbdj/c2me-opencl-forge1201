package com.ishland.c2me.opts.accel.opencl.common.compiler.emitters.conversion;

import com.ishland.c2me.opts.dfc.common.ast.conversion.ToF32Node;
import com.ishland.c2me.opts.dfc.common.gen.meta.ValuesMethodDef;
import com.ishland.c2me.opts.dfc.common.gen.opencl.OpenCLCEmitter;
import com.ishland.c2me.opts.dfc.common.gen.opencl.OpenCLCGenFunctionContext;

public class ToF32NodeOpenCLCEmitter implements OpenCLCEmitter<ToF32Node> {
   public static final ToF32NodeOpenCLCEmitter INSTANCE = new ToF32NodeOpenCLCEmitter();

   private ToF32NodeOpenCLCEmitter() {
   }

   public String doCLGen(ToF32Node node, OpenCLCGenFunctionContext context, String storeTo) {
      ValuesMethodDef next = context.newVar(node.next);
      return storeTo + " = (float) " + context.getDelegateVar(next, next.returnType()) + ";\n";
   }
}
