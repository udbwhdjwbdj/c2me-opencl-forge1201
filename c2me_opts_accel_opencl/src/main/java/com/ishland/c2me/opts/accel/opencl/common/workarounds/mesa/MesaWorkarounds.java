package com.ishland.c2me.opts.accel.opencl.common.workarounds.mesa;

import com.ishland.c2me.opts.accel.opencl.common.enumeration.OpenCLDeviceMetadata;
import com.ishland.c2me.opts.accel.opencl.common.util.CLUtil;

public class MesaWorkarounds {
   public static boolean isRusticl(OpenCLDeviceMetadata metadata) {
      String platformName = CLUtil.getPlatformInfoStringUTF8(metadata.platformPtr, 2306);
      return platformName.trim().equals("rusticl");
   }
}
