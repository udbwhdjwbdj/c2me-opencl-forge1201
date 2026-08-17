package com.ishland.c2me.opts.accel.opencl.common.util;

public class MemoryUtil {
   public static int[] byte2int(byte[] data) {
      if (data == null) {
         return null;
      } else {
         int[] ints = new int[data.length];

         for (int i = 0; i < data.length; i++) {
            ints[i] = data[i] & 255;
         }

         return ints;
      }
   }

   public static int roundUp(int num, int base) {
      int temp = num % base;
      if (temp < 0) {
         temp += base;
      }

      return temp == 0 ? num : num + base - temp;
   }

   public static long roundUp(long num, long base) {
      long temp = num % base;
      if (temp < 0L) {
         temp += base;
      }

      return temp == 0L ? num : num + base - temp;
   }
}
