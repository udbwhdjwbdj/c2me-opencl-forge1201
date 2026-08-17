package com.ishland.c2me.opts.accel.opencl.common.zstd;

import java.io.IOException;
import org.lwjgl.util.zstd.Zstd;

public class ZstdIOException extends IOException {
   private long code;

   public ZstdIOException(long result) {
      this(result, Zstd.ZSTD_getErrorName(result));
   }

   public ZstdIOException(long code, String message) {
      super(message);
      this.code = code;
   }

   public long getErrorCode() {
      return this.code;
   }
}
