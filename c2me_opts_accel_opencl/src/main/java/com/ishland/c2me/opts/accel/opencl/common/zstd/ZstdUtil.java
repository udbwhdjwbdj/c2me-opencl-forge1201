package com.ishland.c2me.opts.accel.opencl.common.zstd;

import org.lwjgl.util.zstd.Zstd;

public class ZstdUtil {
   public static long checkZstdError(long ret) throws ZstdIOException {
      if (Zstd.nZSTD_isError(ret) != 0) {
         throw new ZstdIOException(ret);
      } else {
         return ret;
      }
   }
}
