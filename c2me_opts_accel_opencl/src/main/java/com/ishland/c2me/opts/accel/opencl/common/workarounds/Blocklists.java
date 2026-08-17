package com.ishland.c2me.opts.accel.opencl.common.workarounds;

import com.ishland.c2me.opts.accel.opencl.common.enumeration.OpenCLDeviceMetadata;
import com.ishland.c2me.opts.accel.opencl.common.workarounds.amd.AMDBlocklists;
import com.ishland.c2me.opts.accel.opencl.common.workarounds.intel.IntelBlocklists;
import com.ishland.c2me.opts.accel.opencl.common.workarounds.mesa.MesaBlocklists;
import com.ishland.c2me.opts.accel.opencl.common.workarounds.mesa.MesaWorkarounds;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public class Blocklists {
   public static Set<Blocklists.Reference> getBlockListReasons(OpenCLDeviceMetadata metadata) {
      EnumSet<Blocklists.Reference> set = EnumSet.noneOf(Blocklists.Reference.class);
      if (AMDBlocklists.isUsingBrokenGCNOnOfficialAMDDriver(metadata)) {
         set.add(Blocklists.Reference.BROKEN_DRIVER_AMD_GCN);
      }

      if (AMDBlocklists.isUsingAM5IntegratedGPU(metadata)) {
         set.add(Blocklists.Reference.SLOW_AMD_AM5_IGPU_GFX1036);
      }

      if (IntelBlocklists.isARL_S(metadata)) {
         set.add(Blocklists.Reference.SLOW_INTEL_IGPU_ARL_S);
      }

      if (MesaWorkarounds.isRusticl(metadata) && !MesaBlocklists.isExplicitlyEnabled()) {
         set.add(Blocklists.Reference.RUSTICL_ENABLED_BY_DISTRO);
      }

      return Collections.unmodifiableSet(set);
   }

   public static enum Reference {
      BROKEN_DRIVER_AMD_GCN,
      SLOW_AMD_AM5_IGPU_GFX1036,
      SLOW_INTEL_IGPU_ARL_S,
      RUSTICL_ENABLED_BY_DISTRO;
   }
}
