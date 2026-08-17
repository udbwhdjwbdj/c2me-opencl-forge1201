package com.ishland.c2me.opts.accel.opencl.common.enumeration;

import com.ishland.c2me.opts.accel.opencl.common.Config;
import com.ishland.c2me.opts.accel.opencl.common.util.CLUtil;
import com.ishland.c2me.opts.accel.opencl.common.workarounds.Blocklists;
import io.netty.util.internal.PlatformDependent;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CL;
import org.lwjgl.opencl.CL12;
import org.lwjgl.opencl.CLCapabilities;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenCLDeviceLocator {
   private static final Logger LOGGER = LoggerFactory.getLogger(OpenCLDeviceLocator.class);

   private static void tryLoad(String... libNames) {
      Configuration.OPENCL_EXPLICIT_INIT.set(true);

      try {
         CL.getFunctionProvider();
      } catch (Throwable var9) {
         try {
            CL.create();
         } catch (Throwable var8) {
            LOGGER.error("{}", var8.toString());
            Throwable t = var8;

            for (String libName : libNames) {
               try {
                  CL.create(libName);
                  LOGGER.info("Successfully loaded OpenCL from {}", libName);
                  return;
               } catch (Throwable var7) {
                  LOGGER.error("{}", var7.toString());
                  t.addSuppressed(var7);
               }
            }

            throw new RuntimeException("Failed to load OpenCL", t);
         }
      }
   }

   public static boolean isAvailable() {
      try {
         tryLoad(
            "/usr/lib/x86_64-linux-gnu/libOpenCL.so.1",
            "/usr/lib64/libOpenCL.so.1",
            "/usr/lib/libOpenCL.so.1",
            "/run/opengl-driver/lib/libOpenCL.so",
            "/vendor/lib64/libOpenCL.so"
         );
         return true;
      } catch (Throwable var1) {
         LOGGER.error("Failed to initialize OpenCL", var1);
         if (!Config.allowIncompatibilityFallback) {
            throw var1;
         } else {
            return false;
         }
      }
   }

   public static List<OpenCLDeviceMetadata> enumerateAll() {
      List<OpenCLDeviceMetadata> devices = new ArrayList<>();
      if (!isAvailable()) {
         return devices;
      } else {
         MemoryStack stack = MemoryStack.stackPush();

         try {
            IntBuffer errorCode = stack.mallocInt(1);
            IntBuffer countTmp = stack.mallocInt(1);
            CLUtil.checkCLError(CL12.clGetPlatformIDs(null, countTmp));
            PointerBuffer platforms = stack.mallocPointer(countTmp.get(0));
            CLUtil.checkCLError(CL12.clGetPlatformIDs(platforms, (IntBuffer)null));

            for (int i = 0; i < platforms.capacity(); i++) {
               long platform = platforms.get(i);
               String platformName = CLUtil.getPlatformInfoStringUTF8(platform, 2306);
               String platformVersion = CLUtil.getPlatformInfoStringUTF8(platform, 2305);

               CLCapabilities platformCaps;
               try {
                  platformCaps = CL.createPlatformCapabilities(platform);
               } catch (Throwable var15) {
                  LOGGER.error("Failed to create OpenCL platform capabilities for platform {}", platformName, var15);
                  continue;
               }

               LOGGER.info("Found OpenCL platform {} version {}", platformName, platformVersion);

               List<OpenCLDeviceMetadata> devices1;
               try {
                  devices1 = enumeratePlatformDevices(platform, platformCaps);
               } catch (Throwable var14) {
                  LOGGER.warn("Failed to enumerate OpenCL devices for platform {}", platformName, var14);
                  continue;
               }

               devices.addAll(devices1);
            }
         } catch (Throwable var16) {
            if (stack != null) {
               try {
                  stack.close();
               } catch (Throwable var13) {
                  var16.addSuppressed(var13);
               }
            }

            throw var16;
         }

         if (stack != null) {
            stack.close();
         }

         Iterator<OpenCLDeviceMetadata> iterator = devices.iterator();

         while (iterator.hasNext()) {
            OpenCLDeviceMetadata openCLDeviceMetadata = iterator.next();
            if (!Config.deviceUUIDWhitelist.isEmpty() && !Config.deviceUUIDWhitelist.contains(openCLDeviceMetadata.deviceUUID)) {
               LOGGER.info("Skipping OpenCL device {} since it's not in the whitelist", openCLDeviceMetadata.deviceUUID);
               iterator.remove();
            }

            if (Config.deviceUUIDBlacklist.contains(openCLDeviceMetadata.deviceUUID)) {
               LOGGER.info("Skipping OpenCL device {} since it's in the blacklist", openCLDeviceMetadata.deviceUUID);
               iterator.remove();
            }
         }

         postprocessDeviceList(devices);
         return devices;
      }
   }

   private static List<OpenCLDeviceMetadata> enumeratePlatformDevices(long platform, CLCapabilities platformCaps) {
      List<OpenCLDeviceMetadata> devicesList = new ArrayList<>();
      MemoryStack stack = MemoryStack.stackPush();

      try {
         IntBuffer errorCode = stack.mallocInt(1);
         IntBuffer countTmp = stack.mallocInt(1);
         long clDeviceType = (Config.allowCPUDevices ? 2L : 0L) | (Config.allowGPUDevices ? 4L : 0L) | (Config.allowAcceleratorDevices ? 8L : 0L);
         CLUtil.checkCLError(CL12.clGetDeviceIDs(platform, clDeviceType, null, countTmp));
         PointerBuffer devices = stack.callocPointer(countTmp.get(0));
         CLUtil.checkCLError(CL12.clGetDeviceIDs(platform, clDeviceType, devices, (IntBuffer)null));

         for (int i = 0; i < devices.capacity(); i++) {
            long device = devices.get(i);
            String deviceVendor = CLUtil.getDeviceInfoStringUTF8(device, 4140);
            String deviceName = CLUtil.getDeviceInfoStringUTF8(device, 4139);
            String deviceVersion = CLUtil.getDeviceInfoStringUTF8(device, 4143);
            String deviceExtensions = CLUtil.getDeviceInfoStringUTF8(device, 4144);

            CLCapabilities deviceCaps;
            try {
               deviceCaps = CL.createDeviceCapabilities(device, platformCaps);
            } catch (Throwable var27) {
               LOGGER.error("Failed to create OpenCL device capabilities for device {}", deviceName, var27);
               continue;
            }

            if (!deviceCaps.OpenCL12) {
               LOGGER.warn("OpenCL device ({}) version ({}) does not support OpenCL 1.2", deviceName, deviceVersion);
            } else if (!deviceCaps.cl_khr_fp64) {
               LOGGER.warn("OpenCL device ({}) version ({}) does not support cl_khr_fp64", deviceName, deviceVersion);
            } else {
               String platformVendor = CLUtil.getPlatformInfoStringUTF8(platform, 2307);
               String platformName = CLUtil.getPlatformInfoStringUTF8(platform, 2306);
               String platformVersion = CLUtil.getPlatformInfoStringUTF8(platform, 2305);
               String platformExtensions = CLUtil.getPlatformInfoStringUTF8(platform, 2308);
               UUID deviceUUID;
               UUID driverUUID;
               if (!deviceCaps.cl_khr_device_uuid) {
                  LOGGER.warn("OpenCL device ({}) version ({}) does not support cl_khr_device_uuid, device matching can be unstable", deviceName, deviceVersion);
                  deviceUUID = UUID.nameUUIDFromBytes((deviceVendor + deviceName + deviceVersion + deviceExtensions).getBytes(StandardCharsets.UTF_8));
                  driverUUID = UUID.nameUUIDFromBytes((platformVendor + platformName + platformVersion + platformExtensions).getBytes(StandardCharsets.UTF_8));
               } else {
                  deviceUUID = CLUtil.getDeviceUUID(device);
                  driverUUID = CLUtil.getDriverUUID(device);
               }

               OpenCLDeviceMetadata clDevice = new OpenCLDeviceMetadata(platform, device, platformCaps, deviceCaps, deviceUUID, driverUUID);
               Set<Blocklists.Reference> blockListReasons = Blocklists.getBlockListReasons(clDevice);
               if (!blockListReasons.isEmpty()) {
                  if (!Config.disableBuiltinDeviceBlocklist) {
                     LOGGER.warn("OpenCL device ({}) version ({}) is being blocklisted to prevent crashes or other issues: ", deviceName, deviceVersion);
                     LOGGER.warn("[{}]", blockListReasons.stream().map(Enum::name).collect(Collectors.joining(", ")));
                     continue;
                  }

                  LOGGER.error("OpenCL device ({}) version ({}) is blocklisted, but enabling anyways: ", deviceName, deviceVersion);
                  LOGGER.error("[{}]", blockListReasons.stream().map(Enum::name).collect(Collectors.joining(", ")));
                  LOGGER.error("This may cause crashes or other issues later.");
               }

               LOGGER.info("Found OpenCL device ({}) version ({})", deviceName, deviceVersion);
               LOGGER.info("Device UUID: {}", deviceUUID);
               LOGGER.info("Driver UUID: {}", driverUUID);
               devicesList.add(clDevice);
            }
         }
      } catch (Throwable var28) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var26) {
               var28.addSuppressed(var26);
            }
         }

         throw var28;
      }

      if (stack != null) {
         stack.close();
      }

      return devicesList;
   }

   private static void postprocessDeviceList(List<OpenCLDeviceMetadata> devices) {
      if (PlatformDependent.isWindows()) {
      }
   }

   private static void deselect0(List<OpenCLDeviceMetadata> devices, Predicate<OpenCLDeviceMetadata> whetherDeprioritize, String reason) {
      boolean deprioritize = Config.useDevicePrioritization && devices.stream().anyMatch(metadatax -> !whetherDeprioritize.test(metadatax));
      Iterator<OpenCLDeviceMetadata> iterator = devices.iterator();

      while (iterator.hasNext()) {
         OpenCLDeviceMetadata metadata = iterator.next();
         if (whetherDeprioritize.test(metadata)) {
            String deviceName = CLUtil.getDeviceInfoStringUTF8(metadata.devicePtr, 4139);
            String deviceVersion = CLUtil.getDeviceInfoStringUTF8(metadata.devicePtr, 4143);
            if (deprioritize) {
               LOGGER.warn(
                  "OpenCL device ({}) version ({}) is deselected to prevent crashes or other issues: {}", new Object[]{deviceName, deviceVersion, reason}
               );
               iterator.remove();
            } else {
               LOGGER.warn("OpenCL device ({}) version ({}) have known issues, but enabling anyways: {}", new Object[]{deviceName, deviceVersion, reason});
            }
         }
      }
   }
}
