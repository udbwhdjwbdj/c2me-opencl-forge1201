package com.ishland.c2me.opts.accel.opencl.mixin.workarounds;

import org.lwjgl.opencl.CL;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(
   value = {CL.class},
   remap = false
)
public class MixinCL {
   @ModifyArg(
      method = {"createPlatformCapabilities"},
      at = @At(
         value = "INVOKE",
         target = "Lorg/lwjgl/opencl/CL10;nclGetDeviceIDs(JJIJJ)I"
      ),
      index = 1,
      require = 2
   )
   private static long modifyDeviceType(long device_type) {
      return device_type == -1L ? 4294967295L : device_type;
   }
}
