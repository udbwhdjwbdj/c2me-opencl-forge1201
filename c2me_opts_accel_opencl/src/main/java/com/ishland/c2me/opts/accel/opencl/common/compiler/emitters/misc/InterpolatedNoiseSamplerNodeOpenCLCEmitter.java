package com.ishland.c2me.opts.accel.opencl.common.compiler.emitters.misc;

import com.ishland.c2me.opts.dfc.common.ast.misc.InterpolatedNoiseSamplerNode;
import com.ishland.c2me.opts.dfc.common.gen.opencl.OpenCLCEmitter;
import com.ishland.c2me.opts.dfc.common.gen.opencl.OpenCLCGenFunctionContext;

public class InterpolatedNoiseSamplerNodeOpenCLCEmitter implements OpenCLCEmitter<InterpolatedNoiseSamplerNode> {
   public static final InterpolatedNoiseSamplerNodeOpenCLCEmitter INSTANCE = new InterpolatedNoiseSamplerNodeOpenCLCEmitter();

   private InterpolatedNoiseSamplerNodeOpenCLCEmitter() {
   }

   public String doCLGen(InterpolatedNoiseSamplerNode node, OpenCLCGenFunctionContext context, String storeTo) {
      int offset = context.getGlobalContext().allocGlobalConstDataObject(node.sampler);
      return "global const interpolated_noise_sampler_t * restrict data = ptr_shift_global(ctx.const_data, "
         + offset
         + ");\n"
         + storeTo
         + " = math_noise_perlin_interpolated_sample_global_noinline(data, ctx.x, ctx.y, ctx.z);\n";
   }
}
