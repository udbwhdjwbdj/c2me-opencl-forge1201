package com.ishland.c2me.opts.accel.opencl.common.workarounds.intel;

import com.ishland.c2me.opts.accel.opencl.common.enumeration.OpenCLDeviceMetadata;
import com.ishland.c2me.opts.accel.opencl.common.util.CLUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IntelBlocklists {
   private static final Logger LOGGER = LoggerFactory.getLogger(IntelBlocklists.class);

   public static boolean isARL_S(OpenCLDeviceMetadata metadata) {
      if (!IntelWorkarounds.isUsingIntelGPU(metadata)) {
         return false;
      } else if (!metadata.deviceCaps.cl_intel_device_attribute_query) {
         LOGGER.warn("Unable to determine microarchitecture level for device {}: cl_intel_device_attribute_query not supported", metadata);
         return false;
      } else {
         int pciId = CLUtil.getDeviceInfoInt(metadata.devicePtr, 16977);
         return pciId == 32103 || pciId == 46656;
      }
   }
}
