package com.ishland.c2me.opts.accel.opencl.common.workarounds.amd;

import com.ishland.c2me.opts.accel.opencl.common.enumeration.OpenCLDeviceMetadata;
import com.ishland.c2me.opts.accel.opencl.common.util.CLUtil;
import io.netty.util.internal.PlatformDependent;
import java.nio.IntBuffer;
import org.lwjgl.opencl.CL12;
import org.lwjgl.system.MemoryStack;

public class AMDBlocklists {
   public static boolean isUsingBrokenGCNOnOfficialAMDDriver(OpenCLDeviceMetadata metadata) {
      MemoryStack stack = MemoryStack.stackPush();

      boolean var8;
      label70: {
         boolean var9;
         label71: {
            try {
               IntBuffer vendorIdBuf = stack.callocInt(1);
               CLUtil.checkCLError(CL12.clGetDeviceInfo(metadata.devicePtr, 4097, vendorIdBuf, null));
               int vendorId = vendorIdBuf.get(0);
               if (vendorId != 4098) {
                  var8 = false;
                  break label70;
               }

               String deviceName = CLUtil.getDeviceInfoStringUTF8(metadata.devicePtr, 4139);
               if (!PlatformDependent.isWindows() && deviceName.startsWith("gfx90c")) {
                  var9 = false;
                  break label71;
               }

               var9 = deviceName.startsWith("gfx9")
                  || deviceName.startsWith("gfx8")
                  || deviceName.startsWith("gfx7")
                  || deviceName.startsWith("gfx6")
                  || deviceName.equals("Ellesmere");
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

            return var9;
         }

         if (stack != null) {
            stack.close();
         }

         return var9;
      }

      if (stack != null) {
         stack.close();
      }

      return var8;
   }

   public static boolean isUsingAM5IntegratedGPU(OpenCLDeviceMetadata metadata) {
      MemoryStack stack = MemoryStack.stackPush();

      boolean var8;
      label43: {
         boolean var5;
         try {
            IntBuffer vendorIdBuf = stack.callocInt(1);
            CLUtil.checkCLError(CL12.clGetDeviceInfo(metadata.devicePtr, 4097, vendorIdBuf, null));
            int vendorId = vendorIdBuf.get(0);
            if (vendorId != 4098) {
               var8 = false;
               break label43;
            }

            String deviceName = CLUtil.getDeviceInfoStringUTF8(metadata.devicePtr, 4139);
            var5 = deviceName.equals("gfx1036");
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

      if (stack != null) {
         stack.close();
      }

      return var8;
   }
}
