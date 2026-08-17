package com.ishland.c2me.opts.accel.opencl.common.enumeration;

import com.ishland.c2me.opts.accel.opencl.common.util.CLUtil;
import com.ishland.c2me.opts.accel.opencl.common.workarounds.intel.IntelWorkarounds;
import java.util.Objects;
import java.util.UUID;
import org.lwjgl.opencl.CLCapabilities;

public class OpenCLDeviceMetadata {
   public final long platformPtr;
   public final long devicePtr;
   public final CLCapabilities platformCaps;
   public final CLCapabilities deviceCaps;
   public final UUID deviceUUID;
   public final UUID driverUUID;
   public final boolean supportsNonUniformWorkgroups;
   public final boolean isIntelNeoRuntime;

   OpenCLDeviceMetadata(long platformPtr, long devicePtr, CLCapabilities platformCaps, CLCapabilities deviceCaps, UUID deviceUUID, UUID driverUUID) {
      this.platformPtr = platformPtr;
      this.devicePtr = devicePtr;
      this.platformCaps = platformCaps;
      this.deviceCaps = deviceCaps;
      this.deviceUUID = deviceUUID;
      this.driverUUID = driverUUID;
      this.supportsNonUniformWorkgroups = this.deviceCaps.OpenCL30 && CLUtil.getDeviceInfoBoolean(this.devicePtr, 4197);
      this.isIntelNeoRuntime = IntelWorkarounds.isUsingNEORuntime(this);
   }

   @Override
   public boolean equals(Object object) {
      if (this == object) {
         return true;
      } else if (object != null && this.getClass() == object.getClass()) {
         OpenCLDeviceMetadata that = (OpenCLDeviceMetadata)object;
         return Objects.equals(this.deviceUUID, that.deviceUUID) && Objects.equals(this.driverUUID, that.driverUUID);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.deviceUUID, this.driverUUID);
   }

   @Override
   public String toString() {
      return "OpenCLDeviceMetadata{platformCaps="
         + this.platformCaps
         + ", deviceCaps="
         + this.deviceCaps
         + ", deviceUUID="
         + this.deviceUUID
         + ", driverUUID="
         + this.driverUUID
         + "}";
   }
}
