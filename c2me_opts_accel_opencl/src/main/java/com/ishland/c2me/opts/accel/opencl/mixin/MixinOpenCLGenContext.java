package com.ishland.c2me.opts.accel.opencl.mixin;

import com.ishland.c2me.opts.accel.opencl.ModuleEntryPoint;
import com.ishland.c2me.opts.accel.opencl.common.compiler.GeneratedCLSource;
import com.ishland.c2me.opts.accel.opencl.common.compiler.OpenCLCGen;
import com.ishland.c2me.opts.accel.opencl.common.gen.CLDataUtil;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import java.io.IOException;
import java.io.InputStream;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({OpenCLCGen.ContextImpl.class})
public class MixinOpenCLGenContext {
   @ModifyReturnValue(
      method = {"build"},
      at = {@At("RETURN")},
      remap = false
   )
   private static GeneratedCLSource modifySource(GeneratedCLSource original) {
      try {
         GeneratedCLSource var3;
         try (InputStream in = ModuleEntryPoint.class.getClassLoader().getResourceAsStream("clsources/c2me_opencl_ext_math.cl")) {
            if (in == null) {
               throw new NullPointerException("Resource not found");
            }

            String header = new String(in.readAllBytes());
            var3 = new GeneratedCLSource(
               original.getOrdinal(),
               header + original.getGeneratedSource(),
               original.getConstData(),
               CLDataUtil.transformGlobalDynamicDataOffsets(original.getGlobalDynamicDataOffsets()),
               original.getFlatCachePrefills(),
               original.getCache2dPrefills(),
               original.getInterpolatorPrefills(),
               original.getDefines(),
               original.getBiomeMappings(),
               original.getDumpedPath()
            );
         }

         return var3;
      } catch (IOException var6) {
         throw new RuntimeException(var6);
      }
   }
}
