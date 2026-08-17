package com.ishland.c2me.opts.accel.opencl.common.workarounds.nvidia;

import com.ishland.c2me.opts.accel.opencl.common.enumeration.OpenCLDeviceMetadata;
import com.ishland.c2me.opts.accel.opencl.common.util.CLUtil;
import java.nio.IntBuffer;
import org.lwjgl.opencl.CL12;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NvidiaWorkarounds {
   private static final Logger LOGGER = LoggerFactory.getLogger(NvidiaWorkarounds.class);

   public static boolean isNvidia(OpenCLDeviceMetadata metadata) {
      MemoryStack stack = MemoryStack.stackPush();

      boolean var4;
      try {
         IntBuffer vendorIdBuf = stack.callocInt(1);
         CLUtil.checkCLError(CL12.clGetDeviceInfo(metadata.devicePtr, 4097, vendorIdBuf, null));
         int vendorId = vendorIdBuf.get(0);
         var4 = vendorId == 4318;
      } catch (Throwable var6) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var5) {
               var6.addSuppressed(var5);
            }
         }

         throw var6;
      }

      if (stack != null) {
         stack.close();
      }

      return var4;
   }

   public static boolean isOlderThanSM50(OpenCLDeviceMetadata metadata) {
      if (!isNvidia(metadata)) {
         return false;
      } else if (metadata.deviceCaps.cl_nv_device_attribute_query) {
         int computeCapabilityMajor = CLUtil.getDeviceInfoInt(metadata.devicePtr, 16384);
         return computeCapabilityMajor < 5;
      } else {
         LOGGER.warn("Unable to determine compute capability for device {}: cl_nv_device_attribute_query not supported", metadata);
         return false;
      }
   }
}
