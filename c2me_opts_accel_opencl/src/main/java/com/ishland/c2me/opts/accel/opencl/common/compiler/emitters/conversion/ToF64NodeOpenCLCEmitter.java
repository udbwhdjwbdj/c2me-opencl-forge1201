package com.ishland.c2me.opts.accel.opencl.common.compiler.emitters.conversion;

import com.ishland.c2me.opts.dfc.common.ast.conversion.ToF64Node;
import com.ishland.c2me.opts.dfc.common.gen.meta.ValuesMethodDef;
import com.ishland.c2me.opts.dfc.common.gen.opencl.OpenCLCEmitter;
import com.ishland.c2me.opts.dfc.common.gen.opencl.OpenCLCGenFunctionContext;

public class ToF64NodeOpenCLCEmitter implements OpenCLCEmitter<ToF64Node> {
   public static final ToF64NodeOpenCLCEmitter INSTANCE = new ToF64NodeOpenCLCEmitter();

   private ToF64NodeOpenCLCEmitter() {
   }

   public String doCLGen(ToF64Node node, OpenCLCGenFunctionContext context, String storeTo) {
      ValuesMethodDef next = context.newVar(node.next);
      return storeTo + " = (double) " + context.getDelegateVar(next, next.returnType()) + ";\n";
   }
}
