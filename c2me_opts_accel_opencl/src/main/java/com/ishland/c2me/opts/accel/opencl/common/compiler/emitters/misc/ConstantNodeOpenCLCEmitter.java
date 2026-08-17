package com.ishland.c2me.opts.accel.opencl.common.compiler.emitters.misc;

import com.ishland.c2me.opts.accel.opencl.common.compiler.OpenCLCGen;
import com.ishland.c2me.opts.dfc.common.ast.misc.ConstantNode;
import com.ishland.c2me.opts.dfc.common.gen.opencl.OpenCLCEmitter;
import com.ishland.c2me.opts.dfc.common.gen.opencl.OpenCLCGenFunctionContext;

public class ConstantNodeOpenCLCEmitter implements OpenCLCEmitter<ConstantNode> {
   public static final ConstantNodeOpenCLCEmitter INSTANCE = new ConstantNodeOpenCLCEmitter();

   private ConstantNodeOpenCLCEmitter() {
   }

   public String doCLGen(ConstantNode node, OpenCLCGenFunctionContext context, String storeTo) {
      return storeTo + " = " + OpenCLCGen.literal(node.getValue()) + ";";
   }
}
