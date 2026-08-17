package com.ishland.c2me.opts.accel.opencl.common.util;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.UUID;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CL10;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CLUtil {
   private static final Logger LOGGER = LoggerFactory.getLogger(CLUtil.class);

   public static void checkCLError(IntBuffer errcode) {
      checkCLError(errcode.get(errcode.position()));
   }

   public static void checkCLError(int errcode) {
      if (errcode != 0) {
         throw new RuntimeException(String.format("OpenCL error [%d]", errcode));
      }
   }

   private static void printPlatformInfo(long platform, String param_name, int param) {
      System.out.println(param_name + ": " + getPlatformInfoStringUTF8(platform, param));
   }

   private static void printDeviceInfo(long device, String param_name, int param) {
      System.out.println("\t" + param_name + ": " + getDeviceInfoStringUTF8(device, param));
   }

   public static String getPlatformInfoStringUTF8(long cl_platform_id, int param_name) {
      try {
         MemoryStack stack = MemoryStack.stackPush();

         String var7;
         try {
            PointerBuffer pp = stack.mallocPointer(1);
            checkCLError(CL10.clGetPlatformInfo(cl_platform_id, param_name, (ByteBuffer)null, pp));
            int bytes = (int)pp.get(0);
            ByteBuffer buffer = stack.malloc(bytes);
            checkCLError(CL10.clGetPlatformInfo(cl_platform_id, param_name, buffer, null));
            var7 = MemoryUtil.memUTF8(buffer, bytes - 1);
         } catch (Throwable var9) {
            if (stack != null) {
               try {
                  stack.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (stack != null) {
            stack.close();
         }

         return var7;
      } catch (Throwable var10) {
         LOGGER.error("Failed to get platform info {} for {}", new Object[]{param_name, cl_platform_id, var10});
         return "N/A";
      }
   }

   public static int getDeviceInfoInt(long cl_device_id, int param_name) {
      MemoryStack stack = MemoryStack.stackPush();

      int var5;
      try {
         IntBuffer pl = stack.mallocInt(1);
         checkCLError(CL10.clGetDeviceInfo(cl_device_id, param_name, pl, null));
         var5 = pl.get(0);
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

   public static long getDeviceInfoLong(long cl_device_id, int param_name) {
      MemoryStack stack = MemoryStack.stackPush();

      long var5;
      try {
         LongBuffer pl = stack.mallocLong(1);
         checkCLError(CL10.clGetDeviceInfo(cl_device_id, param_name, pl, null));
         var5 = pl.get(0);
      } catch (Throwable var8) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var7) {
               var8.addSuppressed(var7);
            }
         }

         throw var8;
      }

      if (stack != null) {
         stack.close();
      }

      return var5;
   }

   public static long getDeviceInfoPointer(long cl_device_id, int param_name) {
      MemoryStack stack = MemoryStack.stackPush();

      long var5;
      try {
         PointerBuffer pp = stack.mallocPointer(1);
         checkCLError(CL10.clGetDeviceInfo(cl_device_id, param_name, pp, null));
         var5 = pp.get(0);
      } catch (Throwable var8) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var7) {
               var8.addSuppressed(var7);
            }
         }

         throw var8;
      }

      if (stack != null) {
         stack.close();
      }

      return var5;
   }

   public static String getDeviceInfoStringUTF8(long cl_device_id, int param_name) {
      try {
         MemoryStack stack = MemoryStack.stackPush();

         String var7;
         try {
            PointerBuffer pp = stack.mallocPointer(1);
            checkCLError(CL10.clGetDeviceInfo(cl_device_id, param_name, (ByteBuffer)null, pp));
            int bytes = (int)pp.get(0);
            ByteBuffer buffer = stack.malloc(bytes);
            checkCLError(CL10.clGetDeviceInfo(cl_device_id, param_name, buffer, null));
            var7 = MemoryUtil.memUTF8(buffer, bytes - 1);
         } catch (Throwable var9) {
            if (stack != null) {
               try {
                  stack.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (stack != null) {
            stack.close();
         }

         return var7;
      } catch (Throwable var10) {
         LOGGER.error("Failed to get device info {} for {}", new Object[]{param_name, cl_device_id, var10});
         return "N/A";
      }
   }

   public static UUID getDeviceUUID(long cl_device_id) {
      MemoryStack stack = MemoryStack.stackPush();

      UUID var4;
      try {
         ByteBuffer buffer = stack.malloc(16);
         checkCLError(CL10.clGetDeviceInfo(cl_device_id, 4202, buffer, null));
         buffer.rewind();
         var4 = new UUID(buffer.getLong(), buffer.getLong());
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

   public static UUID getDriverUUID(long cl_device_id) {
      MemoryStack stack = MemoryStack.stackPush();

      UUID var4;
      try {
         ByteBuffer buffer = stack.malloc(16);
         checkCLError(CL10.clGetDeviceInfo(cl_device_id, 4203, buffer, null));
         buffer.rewind();
         var4 = new UUID(buffer.getLong(), buffer.getLong());
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

   public static boolean getDeviceInfoBoolean(long cl_device_id, int param_name) {
      MemoryStack stack = MemoryStack.stackPush();

      boolean var5;
      try {
         IntBuffer pl = stack.callocInt(1);
         checkCLError(CL10.clGetDeviceInfo(cl_device_id, param_name, pl, null));
         var5 = pl.get(0) != 0;
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
}
