package com.ishland.c2me.opts.accel.opencl.common.compiler.emitters.misc;

import com.ishland.c2me.opts.accel.opencl.common.util.BeardifierTables;
import com.ishland.c2me.opts.dfc.common.ast.misc.BeardifierNode;
import com.ishland.c2me.opts.dfc.common.gen.opencl.OpenCLCEmitter;
import com.ishland.c2me.opts.dfc.common.gen.opencl.OpenCLCGenFunctionContext;
import net.minecraft.world.level.levelgen.DensityFunctions.BeardifierMarker;

public class BeardifierNodeOpenCLCEmitter implements OpenCLCEmitter<BeardifierNode> {
   public static final BeardifierNodeOpenCLCEmitter INSTANCE = new BeardifierNodeOpenCLCEmitter();

   private BeardifierNodeOpenCLCEmitter() {
   }

   public String doCLGen(BeardifierNode node, OpenCLCGenFunctionContext context, String storeTo) {
      int offset = context.getGlobalContext().getGlobalDynamicDataOffset(BeardifierMarker.INSTANCE);
      int tableOffset = context.getGlobalContext().allocGlobalConstDataObject(BeardifierTables.getStructureWeightTable());
      return "if (ctx.rw_data) {\n    global const sws_index_t * restrict data = df_data_offset_global(ctx.rw_data, "
         + offset
         + ");\n    global const float * restrict structureWeightSamplerTable = ptr_shift_global(ctx.const_data, "
         + tableOffset
         + ");\n    if (data) {\n        "
         + storeTo
         + " = df_structureWeightSampler_sample(structureWeightSamplerTable, data, ctx.x, ctx.y, ctx.z);\n    } else { \n        "
         + storeTo
         + " = 0.0;\n    }\n} else {\n    "
         + storeTo
         + " = 0.0;\n}\n";
   }
}
