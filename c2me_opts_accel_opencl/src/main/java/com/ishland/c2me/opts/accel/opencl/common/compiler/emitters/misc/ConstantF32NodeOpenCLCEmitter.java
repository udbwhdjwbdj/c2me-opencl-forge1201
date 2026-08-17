package com.ishland.c2me.opts.accel.opencl.common.compiler.emitters.misc;

import com.ishland.c2me.opts.accel.opencl.common.compiler.OpenCLCGen;
import com.ishland.c2me.opts.dfc.common.ast.misc.ConstantF32Node;
import com.ishland.c2me.opts.dfc.common.gen.opencl.OpenCLCEmitter;
import com.ishland.c2me.opts.dfc.common.gen.opencl.OpenCLCGenFunctionContext;

public class ConstantF32NodeOpenCLCEmitter implements OpenCLCEmitter<ConstantF32Node> {
   public static final ConstantF32NodeOpenCLCEmitter INSTANCE = new ConstantF32NodeOpenCLCEmitter();

   private ConstantF32NodeOpenCLCEmitter() {
   }

   public String doCLGen(ConstantF32Node node, OpenCLCGenFunctionContext context, String storeTo) {
      return storeTo + " = " + OpenCLCGen.literal(node.getValue()) + ";";
   }
}
