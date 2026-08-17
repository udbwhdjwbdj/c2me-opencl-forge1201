package com.ishland.c2me.opts.accel.opencl.common.workarounds;

import com.ishland.c2me.opts.accel.opencl.common.enumeration.OpenCLDeviceMetadata;
import com.ishland.c2me.opts.accel.opencl.common.workarounds.intel.IntelWorkarounds;
import com.ishland.c2me.opts.accel.opencl.common.workarounds.mesa.MesaWorkarounds;
import com.ishland.c2me.opts.accel.opencl.common.workarounds.nvidia.NvidiaWorkarounds;
import io.netty.util.internal.PlatformDependent;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public class Workarounds {
   public static Set<Workarounds.Reference> getWorkarounds(OpenCLDeviceMetadata metadata) {
      EnumSet<Workarounds.Reference> set = EnumSet.noneOf(Workarounds.Reference.class);
      if (IntelWorkarounds.isUsingIntelOnLinux(metadata)) {
         set.add(Workarounds.Reference.INTEL_LINUX_CLEANUP_HANG);
      }

      if (IntelWorkarounds.isUsingIntelOnWindows(metadata)) {
         if (IntelWorkarounds.isUsingGen9OnWindows(metadata)) {
            set.add(Workarounds.Reference.BUILTIN_TRAP_BROKEN);
            set.add(Workarounds.Reference.INTEL_STATIC_PROFILE_GUIDED_TRIMMING_UNAVAILABLE);
         }

         set.add(Workarounds.Reference.INTEL_IGC_OPTS_UNAVAILABLE);
      }

      if (Boolean.getBoolean("com.ishland.c2me.opts.accel.opencl.markBuiltinTrapBroken")) {
         set.add(Workarounds.Reference.BUILTIN_TRAP_BROKEN);
      }

      if (NvidiaWorkarounds.isNvidia(metadata)) {
         set.add(Workarounds.Reference.NVIDIA_INCOMPLETE_CL30_IMPLEMENTATION);
         if (NvidiaWorkarounds.isOlderThanSM50(metadata)) {
            set.add(Workarounds.Reference.NVIDIA_FAST_COMPILE_UNAVAILABLE);
         }

         if (!PlatformDependent.isWindows()) {
            set.add(Workarounds.Reference.NVIDIA_LINUX_HANG_ON_TOO_MANY_BATCHES);
         }
      }

      if (MesaWorkarounds.isRusticl(metadata)) {
         set.add(Workarounds.Reference.REQUIRE_EXPLICIT_FLUSHES);
      }

      return Collections.unmodifiableSet(set);
   }

   public static enum Reference {
      BUILTIN_TRAP_BROKEN,
      NVIDIA_INCOMPLETE_CL30_IMPLEMENTATION,
      NVIDIA_FAST_COMPILE_UNAVAILABLE,
      NVIDIA_LINUX_HANG_ON_TOO_MANY_BATCHES,
      INTEL_LINUX_CLEANUP_HANG,
      INTEL_IGC_OPTS_UNAVAILABLE,
      INTEL_STATIC_PROFILE_GUIDED_TRIMMING_UNAVAILABLE,
      REQUIRE_EXPLICIT_FLUSHES;
   }
}
