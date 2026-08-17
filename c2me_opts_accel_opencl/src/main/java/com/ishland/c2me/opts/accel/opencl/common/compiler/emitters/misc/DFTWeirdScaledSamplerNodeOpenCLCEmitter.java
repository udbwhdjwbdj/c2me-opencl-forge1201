package com.ishland.c2me.opts.accel.opencl.common.compiler.emitters.misc;

import com.ishland.c2me.opts.dfc.common.ast.noise.DFTWeirdScaledSamplerNode;
import com.ishland.c2me.opts.dfc.common.gen.meta.ValuesMethodDefF64;
import com.ishland.c2me.opts.dfc.common.gen.opencl.OpenCLCEmitter;
import com.ishland.c2me.opts.dfc.common.gen.opencl.OpenCLCGenFunctionContext;

public class DFTWeirdScaledSamplerNodeOpenCLCEmitter implements OpenCLCEmitter<DFTWeirdScaledSamplerNode> {
   public static final DFTWeirdScaledSamplerNodeOpenCLCEmitter INSTANCE = new DFTWeirdScaledSamplerNodeOpenCLCEmitter();

   private DFTWeirdScaledSamplerNodeOpenCLCEmitter() {
   }

   public String doCLGen(DFTWeirdScaledSamplerNode node, OpenCLCGenFunctionContext context, String storeTo) {
      ValuesMethodDefF64 inputMethod = context.newVarF64(node.input);
      StringBuilder builder = new StringBuilder();
      if (node.noise.noise() != null) {
         int offset = context.getGlobalContext().allocGlobalConstDataObject(node.noise.noise());
         builder.append("double v = ").append(context.getDelegateVar(inputMethod)).append(";\n");
         switch (node.mapper) {
            case TYPE1:
               builder.append("double d = df_caveScaler_scaleTunnels(v);\n");
               break;
            case TYPE2:
               builder.append("double d = df_caveScaler_scaleCaves(v);\n");
         }

         builder.append("global const double_octave_sampler_data_t * restrict data = ptr_shift_global(ctx.const_data, ").append(offset).append(");\n");
         builder.append(storeTo)
            .append(" = d * fabs(math_noise_perlin_double_octave_sample_global_noinline(data, (double) ctx.x / d, (double) ctx.y / d, (double) ctx.z / d));\n");
      } else {
         builder.append("double v = ").append(context.getDelegateVar(inputMethod)).append(";\n");
         builder.append(storeTo).append(" = d * 0.0;\n");
      }

      return builder.toString();
   }
}
