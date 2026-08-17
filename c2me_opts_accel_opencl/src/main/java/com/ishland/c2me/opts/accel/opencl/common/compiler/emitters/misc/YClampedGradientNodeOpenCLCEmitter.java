package com.ishland.c2me.opts.accel.opencl.common.compiler.emitters.misc;

import com.ishland.c2me.opts.accel.opencl.common.compiler.OpenCLCGen;
import com.ishland.c2me.opts.dfc.common.ast.misc.YClampedGradientNode;
import com.ishland.c2me.opts.dfc.common.gen.opencl.OpenCLCEmitter;
import com.ishland.c2me.opts.dfc.common.gen.opencl.OpenCLCGenFunctionContext;

public class YClampedGradientNodeOpenCLCEmitter implements OpenCLCEmitter<YClampedGradientNode> {
   public static final YClampedGradientNodeOpenCLCEmitter INSTANCE = new YClampedGradientNodeOpenCLCEmitter();

   private YClampedGradientNodeOpenCLCEmitter() {
   }

   public String doCLGen(YClampedGradientNode node, OpenCLCGenFunctionContext context, String storeTo) {
      return storeTo
         + " = math_clampedMap((double) ctx.y, "
         + OpenCLCGen.literal(node.fromY)
         + ", "
         + OpenCLCGen.literal(node.toY)
         + ", "
         + OpenCLCGen.literal(node.fromValue)
         + ", "
         + OpenCLCGen.literal(node.toValue)
         + ");\n";
   }
}
