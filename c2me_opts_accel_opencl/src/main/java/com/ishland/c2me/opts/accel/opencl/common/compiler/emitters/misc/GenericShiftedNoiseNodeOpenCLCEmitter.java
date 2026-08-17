package com.ishland.c2me.opts.accel.opencl.common.compiler.emitters.misc;

import com.ishland.c2me.opts.dfc.common.ast.noise.GenericShiftedNoiseNode;
import com.ishland.c2me.opts.dfc.common.gen.meta.ValuesMethodDefF64;
import com.ishland.c2me.opts.dfc.common.gen.opencl.OpenCLCEmitter;
import com.ishland.c2me.opts.dfc.common.gen.opencl.OpenCLCGenFunctionContext;

public class GenericShiftedNoiseNodeOpenCLCEmitter implements OpenCLCEmitter<GenericShiftedNoiseNode> {
   public static final GenericShiftedNoiseNodeOpenCLCEmitter INSTANCE = new GenericShiftedNoiseNodeOpenCLCEmitter();

   private GenericShiftedNoiseNodeOpenCLCEmitter() {
   }

   public String doCLGen(GenericShiftedNoiseNode node, OpenCLCGenFunctionContext context, String storeTo) {
      if (node.noise.noise() == null) {
         return storeTo + " = 0.0;\n";
      } else {
         ValuesMethodDefF64 inputXMethod = context.newVarF64(node.inputX);
         ValuesMethodDefF64 inputYMethod = context.newVarF64(node.inputY);
         ValuesMethodDefF64 inputZMethod = context.newVarF64(node.inputZ);
         int offset = context.getGlobalContext().allocGlobalConstDataObject(node.noise.noise());
         return "global const double_octave_sampler_data_t * restrict data = ptr_shift_global(ctx.const_data, "
            + offset
            + ");\n"
            + storeTo
            + " = math_noise_perlin_double_octave_sample_global_noinline(data, "
            + context.getDelegateVar(inputXMethod)
            + ","
            + context.getDelegateVar(inputYMethod)
            + ","
            + context.getDelegateVar(inputZMethod)
            + ");\n";
      }
   }
}
