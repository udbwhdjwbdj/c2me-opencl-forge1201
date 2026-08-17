package com.ishland.c2me.opts.accel.opencl;

import com.ishland.c2me.base.common.config.ConfigSystem.ConfigAccessor;
import com.ishland.c2me.opts.accel.opencl.common.Config;

public class ModuleEntryPoint {
   private static final boolean enabled = new ConfigAccessor()
      .key("openclAccel.enabled")
      .comment("Enable OpenCL acceleration for world generation.\n")
      .getBoolean(true, false);

   static {
      Config.init();
   }
}
