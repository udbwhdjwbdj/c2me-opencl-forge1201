package com.ishland.c2me.opts.accel.opencl.common.compiler.emitters.misc;

import com.ishland.c2me.opts.dfc.common.ast.misc.CoordinateNode;
import com.ishland.c2me.opts.dfc.common.gen.opencl.OpenCLCEmitter;
import com.ishland.c2me.opts.dfc.common.gen.opencl.OpenCLCGenFunctionContext;

public class CoordinateNodeOpenCLCEmitter implements OpenCLCEmitter<CoordinateNode> {
   public static final CoordinateNodeOpenCLCEmitter INSTANCE = new CoordinateNodeOpenCLCEmitter();

   private CoordinateNodeOpenCLCEmitter() {
   }

   public String doCLGen(CoordinateNode node, OpenCLCGenFunctionContext context, String storeTo) {
      return switch (node.axis) {
         case X -> storeTo + " = (double) ctx.x;\n";
         case Y -> storeTo + " = (double) ctx.y;\n";
         case Z -> storeTo + " = (double) ctx.z;\n";
         default -> throw new IllegalStateException("Unexpected type");
      };
   }
}
