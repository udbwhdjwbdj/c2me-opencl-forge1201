package com.ishland.c2me.opts.accel.opencl.common.workarounds.mesa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MesaBlocklists {
   private static final Logger LOGGER = LoggerFactory.getLogger(MesaBlocklists.class);

   public static boolean isExplicitlyEnabled() {
      return System.getenv("RUSTICL_ENABLE") != null;
   }
}
