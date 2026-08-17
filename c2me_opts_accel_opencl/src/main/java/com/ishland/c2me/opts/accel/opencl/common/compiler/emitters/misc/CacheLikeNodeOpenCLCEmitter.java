package com.ishland.c2me.opts.accel.opencl.common.compiler.emitters.misc;

import com.ishland.c2me.opts.accel.opencl.common.compiler.OpenCLCGen;
import com.ishland.c2me.opts.dfc.common.ast.misc.CacheLikeNode;
import com.ishland.c2me.opts.dfc.common.gen.meta.ValuesMethodDefF64;
import com.ishland.c2me.opts.dfc.common.gen.opencl.OpenCLCEmitter;
import com.ishland.c2me.opts.dfc.common.gen.opencl.OpenCLCGenFunctionContext;
import net.minecraft.world.level.levelgen.DensityFunctions.MarkerOrMarked;

public class CacheLikeNodeOpenCLCEmitter implements OpenCLCEmitter<CacheLikeNode> {
   public static final CacheLikeNodeOpenCLCEmitter INSTANCE = new CacheLikeNodeOpenCLCEmitter();

   private CacheLikeNodeOpenCLCEmitter() {
   }

   public String doCLGen(CacheLikeNode node, OpenCLCGenFunctionContext context, String storeTo) {
      if (node.getCacheLike() instanceof MarkerOrMarked wrapping) {
         String var10000;
         switch (wrapping.type()) {
            case CacheOnce:
            case CacheAllInCell:
               ValuesMethodDefF64 valuesMethodDefF64 = context.newVarF64(node.getDelegate());
               var10000 = storeTo + " = " + context.getDelegateVar(valuesMethodDefF64) + ";\n";
               break;
            case Interpolated:
               if (context.getVariant().enableAllCache) {
                  int offset = context.getGlobalContext().getGlobalDynamicDataOffset(OpenCLCGen.MARKER_cacheLike_interpolator);
                  int ordinal = context.getGlobalContext().registerInterpolator(node);
                  var10000 = "if (ctx.rw_data && (ctx.sample_flags & MASK_enableAllCaches) == MASK_enableAllCaches) {\n    global const worldgen_params_t * restrict params = ctx.rw_data;\n    global double * restrict interpolator_buffer = df_data_offset_global(ctx.rw_data, "
                     + offset
                     + ");\n    const cache_result_t res = df_cachelike_interpolator(params, interpolator_buffer, "
                     + ordinal
                     + ", ctx.x, ctx.y, ctx.z, ctx.sample_flags);\n    if (res.cached) {\n        "
                     + storeTo
                     + " = res.res;\n    } else {\n        df_cachelike_trap_printf(\"interpolator\", ctx);\n        __builtin_trap();\n        __builtin_unreachable();\n        "
                     + storeTo
                     + " = nan((uint64_t) 0);\n    }\n} else {\n    df_cachelike_trap_printf(\"interpolator\", ctx);\n    __builtin_trap();\n    __builtin_unreachable();\n    "
                     + storeTo
                     + " = nan((uint64_t) 0);\n}\n";
               } else {
                  ValuesMethodDefF64 valuesMethodDefF64x = context.newVarF64(node.getDelegate());
                  var10000 = storeTo + " = " + context.getDelegateVar(valuesMethodDefF64x) + ";\n";
               }
               break;
            case FlatCache:
               if (context.getVariant().enableFlatCache) {
                  int offset = context.getGlobalContext().getGlobalDynamicDataOffset(OpenCLCGen.MARKER_cacheLike_flatCache);
                  int ordinal = context.getGlobalContext().registerFlatCache(node);
                  var10000 = "if (ctx.rw_data && (ctx.sample_flags & MASK_enableFlatCache) == MASK_enableFlatCache) {\n    global const worldgen_params_t * restrict params = ctx.rw_data;\n    global const double * restrict data = df_data_offset_global(ctx.rw_data, "
                     + offset
                     + ");\n    const cache_result_t res = df_cachelike_flatcache(params, data, "
                     + ordinal
                     + ", ctx.x, ctx.y, ctx.z, ctx.sample_flags);\n    if (res.cached) {\n        "
                     + storeTo
                     + " =  res.res;\n    } else {\n        df_cachelike_trap_printf(\"flatcache\", ctx);\n        __builtin_trap();\n        __builtin_unreachable();\n        "
                     + storeTo
                     + " = nan((uint64_t) 0);\n    }\n} else {\n    df_cachelike_trap_printf(\"flatcache\", ctx);\n    __builtin_trap();\n    __builtin_unreachable();\n    "
                     + storeTo
                     + " = nan((uint64_t) 0);\n}\n";
               } else {
                  ValuesMethodDefF64 valuesMethodDefF64x = context.newVarF64(node.getDelegate());
                  var10000 = storeTo + " = " + context.getDelegateVar(valuesMethodDefF64x) + ";\n";
               }
               break;
            case Cache2D:
               if (context.getVariant().useCache2D()) {
                  int offset = context.getGlobalContext().getGlobalDynamicDataOffset(OpenCLCGen.MARKER_cacheLike_cache2d);
                  int ordinal = context.getGlobalContext().registerCache2d(node);
                  var10000 = "if (ctx.rw_data && (ctx.sample_flags & MASK_enableAllCaches) == MASK_enableAllCaches) {\n    global const worldgen_params_t * restrict params = ctx.rw_data;\n    global const double * restrict data = df_data_offset_global(ctx.rw_data, "
                     + offset
                     + ");\n    const cache_result_t res = df_cachelike_cache2d(params, data, "
                     + ordinal
                     + ", ctx.x, ctx.y, ctx.z, ctx.sample_flags);\n    if (res.cached) {\n        "
                     + storeTo
                     + " = res.res;\n    } else {\n        df_cachelike_trap_printf(\"cache2d\", ctx);\n        __builtin_trap();\n        __builtin_unreachable();\n        "
                     + storeTo
                     + " = nan((uint64_t) 0);\n    }\n} else {\n    df_cachelike_trap_printf(\"cache2d\", ctx);\n    __builtin_trap();\n    __builtin_unreachable();\n    "
                     + storeTo
                     + " = nan((uint64_t) 0);\n}\n";
               } else {
                  ValuesMethodDefF64 valuesMethodDefF64x = context.newVarF64(node.getDelegate());
                  var10000 = storeTo + " = " + context.getDelegateVar(valuesMethodDefF64x) + ";\n";
               }
               break;
            default:
               throw new IllegalStateException("Unexpected type");
         }

         return var10000;
      } else {
         throw new UnsupportedOperationException("Can only gen wrapping");
      }
   }
}
