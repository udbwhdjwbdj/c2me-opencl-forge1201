package com.ishland.c2me.opts.accel.opencl.common.compiler.emitters.misc;

import com.ishland.c2me.opts.accel.opencl.mixin.access.IDensityFunctionTypesEndIslands;
import com.ishland.c2me.opts.accel.opencl.mixin.access.ISimplexNoiseSampler;
import com.ishland.c2me.opts.dfc.common.ast.misc.EndIslandsNode;
import com.ishland.c2me.opts.dfc.common.gen.opencl.OpenCLCEmitter;
import com.ishland.c2me.opts.dfc.common.gen.opencl.OpenCLCGenFunctionContext;

public class EndIslandsNodeOpenCLCEmitter implements OpenCLCEmitter<EndIslandsNode> {
   public static final EndIslandsNodeOpenCLCEmitter INSTANCE = new EndIslandsNodeOpenCLCEmitter();

   private EndIslandsNodeOpenCLCEmitter() {
   }

   public String doCLGen(EndIslandsNode node, OpenCLCGenFunctionContext context, String storeTo) {
      int[] permutation = ((ISimplexNoiseSampler)((IDensityFunctionTypesEndIslands)(Object)node.endIslands).getSampler()).getPermutation();
      int offset = context.getGlobalContext().allocGlobalConstDataObject(permutation);
      return "global const uint32_t * const permutation = ptr_shift_global(ctx.const_data, "
         + offset
         + ");\n"
         + storeTo
         + " = ((double) math_end_islands_sample_global(permutation, ctx.x / 8, ctx.z / 8) - 8.0) / 128.0;\n";
   }
}
