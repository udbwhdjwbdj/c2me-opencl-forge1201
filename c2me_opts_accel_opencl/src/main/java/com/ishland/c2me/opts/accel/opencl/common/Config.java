package com.ishland.c2me.opts.accel.opencl.common;

import com.ishland.c2me.base.common.config.ConfigSystem.ConfigAccessor;
import com.ishland.c2me.base.common.config.ConfigSystem.LongChecks;
import java.util.HashSet;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Config {
   private static final Logger LOGGER = LoggerFactory.getLogger(Config.class);
   public static final int maxConcurrentTasksPerDevice = (int)new ConfigAccessor()
      .key("openclAccel.maxConcurrentTasksPerDevice")
      .comment("Maximum number of concurrent tasks per device\nIncreasing this may increase the performance and will increase the VRAM usage\n")
      .getLong(32L, 32L, new LongChecks[]{LongChecks.THREAD_COUNT});
   public static final boolean lowPriorityQueues = new ConfigAccessor()
      .key("openclAccel.lowPriorityQueues")
      .comment(
         "Whether to attempt to lower the priority of the command queues\nThis may reduce the fps drop caused by the OpenCL acceleration\n\nRequires cl_khr_priority_hints and cl_khr_throttle_hints\n"
      )
      .getBoolean(true, true);
   public static final boolean preferFastCompilation = new ConfigAccessor()
      .key("openclAccel.preferFastCompilation")
      .comment("Whether to prefer fast compilation with noinline hints\n")
      .getBoolean(true, true);
   public static final boolean allowCPUDevices = new ConfigAccessor()
      .key("openclAccel.allowCPUDevices")
      .comment("Whether to allow the usage of CPU OpenCL devices\n")
      .getBoolean(false, false);
   public static final boolean allowGPUDevices = new ConfigAccessor()
      .key("openclAccel.allowGPUDevices")
      .comment("Whether to allow the usage of GPU OpenCL devices\n")
      .getBoolean(true, false);
   public static final boolean allowAcceleratorDevices = new ConfigAccessor()
      .key("openclAccel.allowAcceleratorDevices")
      .comment("Whether to allow the usage of Accelerator OpenCL devices\n")
      .getBoolean(true, false);
   public static final boolean allowIncompatibilityFallback = new ConfigAccessor()
      .key("openclAccel.allowIncompatibilityFallback")
      .comment("Whether to allow falling back to non-OpenCL world generation if OpenCL initialization fails\n")
      .getBoolean(false, false);
   public static final boolean disableBuiltinDeviceBlocklist = new ConfigAccessor()
      .key("openclAccel.disableBuiltinDeviceBlocklist")
      .comment("Overrides the built-in blocklist and enables acceleration on unsupported configurations\n")
      .getBoolean(false, false);
   public static final boolean preserveAllControlFlows = new ConfigAccessor()
      .key("openclAccel.preserveAllControlFlows")
      .comment(
         "Uses old compiler behavior of preserving all control flows in the generated OpenCL code.\nThis will increase memory pressure and time used when compiling, but will usually produce faster code.\n"
      )
      .getBoolean(true, false);
   public static final boolean enableIntelFastCompilation = new ConfigAccessor()
      .key("openclAccel.enableIntelFastCompilation")
      .comment("Enables fast compilation options for intel GPUs\n")
      .getBoolean(false, false);
   public static final boolean enableNvidiaFastCompilation = new ConfigAccessor()
      .key("openclAccel.enableNvidiaFastCompilation")
      .comment("Enables fast compilation options for nvidia GPUs\n\nThis *will* decrease GPU-bound throughput by roughly 20%\n")
      .getBoolean(true, false);
   public static final boolean useSmallerBatches = new ConfigAccessor()
      .key("openclAccel.useSmallerBatches")
      .comment(
         "Whether to use smaller batches in worldgen\n\nSmaller batches helps older iGPUs to avoid tripping GPU timeouts,\nbut it *will* reduce scheduling efficiency for modern GPUs\n"
      )
      .getBoolean(false, false);
   public static final boolean useDevicePrioritization = new ConfigAccessor()
      .key("openclAccel.useDevicePrioritization")
      .comment("Whether to use the default device prioritization strategy\n\nCurrently does nothing because most outstanding issues have been resolved.\n")
      .getBoolean(true, false);
   public static final boolean doExplicitFlushes = new ConfigAccessor()
      .key("openclAccel.doExplicitFlushes")
      .comment("Whether to perform explicit flushes after every batch\n")
      .getBoolean(false, false);
   public static final String deviceUUIDBlacklistRaw = new ConfigAccessor()
      .key("openclAccel.deviceUUIDBlacklist")
      .comment(
         "A comma-separated list of device UUIDs to blacklist\nYou can find the UUIDs in the log when the OpenCL devices are enumerated\nExample: \"951e5ce5-ccec-4a37-9ece-d0a800662d8f,3e825b77-fa62-403e-9155-aed1c014b2a2\"\n"
      )
      .getString("", "");
   public static final String deviceUUIDWhitelistRaw = new ConfigAccessor()
      .key("openclAccel.deviceUUIDWhitelist")
      .comment(
         "A comma-separated list of device UUIDs to whitelist\nIf non-empty, only the devices in this list will be used\nYou can find the UUIDs in the log when the OpenCL devices are enumerated\nExample: \"951e5ce5-ccec-4a37-9ece-d0a800662d8f,3e825b77-fa62-403e-9155-aed1c014b2a2\"\n"
      )
      .getString("", "");
   public static final HashSet<UUID> deviceUUIDBlacklist = new HashSet<>();
   public static final HashSet<UUID> deviceUUIDWhitelist = new HashSet<>();

   public static void init() {
      tryChunkyMaxWorkingCount();
   }

   private static void tryChunkyMaxWorkingCount() {
      String chunkyMaxWorkingCount = System.getProperty("chunky.maxWorkingCount", "");

      int value;
      try {
         value = Integer.parseInt(chunkyMaxWorkingCount);
      } catch (NumberFormatException var3) {
         value = 0;
      }

      if (value == 0) {
         if (Runtime.getRuntime().maxMemory() > 10737418240L) {
            value = 512;
         } else {
            value = 192;
         }

         System.setProperty("chunky.maxWorkingCount", Integer.toString(value));
      }

      LOGGER.info("chunky.maxWorkingCount: {}", value);
   }

   static {
      for (String s : deviceUUIDBlacklistRaw.split(",")) {
         String trimmed = s.trim();
         if (!trimmed.isEmpty()) {
            try {
               deviceUUIDBlacklist.add(UUID.fromString(trimmed));
            } catch (IllegalArgumentException var7) {
               LOGGER.error("Invalid UUID in openclAccel.deviceUUIDBlacklist: {}", trimmed);
            }
         }
      }

      for (String sx : deviceUUIDWhitelistRaw.split(",")) {
         String trimmed = sx.trim();
         if (!trimmed.isEmpty()) {
            try {
               deviceUUIDWhitelist.add(UUID.fromString(trimmed));
            } catch (IllegalArgumentException var6) {
               LOGGER.error("Invalid UUID in openclAccel.deviceUUIDWhitelist: {}", trimmed);
            }
         }
      }
   }
}
