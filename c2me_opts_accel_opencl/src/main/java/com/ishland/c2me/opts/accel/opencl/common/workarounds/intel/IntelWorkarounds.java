package com.ishland.c2me.opts.accel.opencl.common.workarounds.intel;

import com.ishland.c2me.opts.accel.opencl.common.enumeration.OpenCLDeviceMetadata;
import com.ishland.c2me.opts.accel.opencl.common.util.CLUtil;
import io.netty.util.internal.PlatformDependent;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IntelWorkarounds {
   private static final Logger LOGGER = LoggerFactory.getLogger(IntelWorkarounds.class);
   public static final int DUMMY_GEN9_IP_VERSION = 589824;
   public static final IntSet gen9IPs = IntSet.of(new int[]{37748745, 37765129, 37781513, 37797888, 37814272, 37830656, 37847040, 37863424, 589824});

   public static boolean isUsingIntelGPU(OpenCLDeviceMetadata metadata) {
      MemoryStack stack = MemoryStack.stackPush();

      boolean var5;
      try {
         long deviceType = CLUtil.getDeviceInfoLong(metadata.devicePtr, 4096);
         int vendorId = CLUtil.getDeviceInfoInt(metadata.devicePtr, 4097);
         var5 = deviceType == 4L && vendorId == 32902;
      } catch (Throwable var7) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var6) {
               var7.addSuppressed(var6);
            }
         }

         throw var7;
      }

      if (stack != null) {
         stack.close();
      }

      return var5;
   }

   public static boolean isUsingNEORuntime(OpenCLDeviceMetadata metadata) {
      if (!isUsingIntelGPU(metadata)) {
         return false;
      } else {
         String deviceVersion = CLUtil.getDeviceInfoStringUTF8(metadata.devicePtr, 4143);
         return deviceVersion.contains("NEO");
      }
   }

   public static boolean isUsingGen9OnWindows(OpenCLDeviceMetadata metadata) {
      return PlatformDependent.isWindows() && isUsingGen9(metadata);
   }

   public static boolean isUsingGen9(OpenCLDeviceMetadata metadata) {
      if (!isUsingIntelGPU(metadata)) {
         return false;
      } else if (metadata.deviceCaps.cl_intel_device_attribute_query) {
         int ipVersion = CLUtil.getDeviceInfoInt(metadata.devicePtr, 16976);
         return gen9IPs.contains(ipVersion);
      } else {
         LOGGER.warn("Unable to determine microarchitecture level for device {}: cl_intel_device_attribute_query not supported", metadata);
         String deviceName = CLUtil.getDeviceInfoStringUTF8(metadata.devicePtr, 4139);
         return deviceName.equals("Intel(R) HD Graphics 500")
            || deviceName.equals("Intel(R) HD Graphics 505")
            || deviceName.equals("Intel(R) HD Graphics 510")
            || deviceName.equals("Intel(R) HD Graphics 515")
            || deviceName.equals("Intel(R) HD Graphics 520")
            || deviceName.equals("Intel(R) HD Graphics 530")
            || deviceName.equals("Intel(R) HD Graphics P530")
            || deviceName.equals("Intel(R) Iris(R) Graphics 540")
            || deviceName.equals("Intel(R) Iris(R) Graphics 550")
            || deviceName.equals("Intel(R) Iris(TM) Pro Graphics 580")
            || deviceName.equals("Intel(R) Iris(TM) Pro Graphics P555")
            || deviceName.equals("Intel(R) Iris(TM) Pro Graphics P580")
            || deviceName.equals("Intel(R) HD Graphics 610")
            || deviceName.equals("Intel(R) HD Graphics 615")
            || deviceName.equals("Intel(R) HD Graphics 620")
            || deviceName.equals("Intel(R) HD Graphics 630")
            || deviceName.equals("Intel(R) HD Graphics P630")
            || deviceName.equals("Intel(R) Iris(R) Plus Graphics")
            || deviceName.equals("Intel(R) Iris(R) Plus Graphics 640")
            || deviceName.equals("Intel(R) Iris(R) Plus Graphics 645")
            || deviceName.equals("Intel(R) Iris(R) Plus Graphics 650")
            || deviceName.equals("Intel(R) Iris(R) Plus Graphics 655")
            || deviceName.equals("Intel(R) UHD Graphics")
            || deviceName.equals("Intel(R) UHD Graphics 600")
            || deviceName.equals("Intel(R) UHD Graphics 605")
            || deviceName.equals("Intel(R) UHD Graphics 610")
            || deviceName.equals("Intel(R) UHD Graphics 615")
            || deviceName.equals("Intel(R) UHD Graphics 617")
            || deviceName.equals("Intel(R) UHD Graphics 620")
            || deviceName.equals("Intel(R) UHD Graphics 630")
            || deviceName.equals("Intel(R) UHD Graphics P630");
      }
   }

   public static boolean isUsingIntelOnLinux(OpenCLDeviceMetadata metadata) {
      return !PlatformDependent.isWindows() && isUsingIntelGPU(metadata);
   }

   public static boolean isUsingIntelOnWindows(OpenCLDeviceMetadata metadata) {
      return PlatformDependent.isWindows() && isUsingIntelGPU(metadata);
   }
}
