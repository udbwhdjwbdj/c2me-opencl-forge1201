package com.ishland.c2me.opts.accel.opencl.common.zstd;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.zstd.ZSTDInBuffer;
import org.lwjgl.util.zstd.ZSTDOutBuffer;
import org.lwjgl.util.zstd.Zstd;

public class ZstdInputStreamNoFinalizer extends FilterInputStream {
   private final long stream;
   private long srcPos = 0L;
   private long srcSize = 0L;
   private boolean needRead = true;
   private final ByteBuffer srcByteBuffer;
   private final byte[] srcTempArray;
   private static final int srcBuffSize = (int)Zstd.ZSTD_DStreamInSize();
   private final ZSTDInBuffer zstdInBuffer;
   private final ZSTDOutBuffer zstdOutBuffer;
   private boolean isContinuous = false;
   private boolean frameFinished = true;
   private boolean isClosed = false;

   public ZstdInputStreamNoFinalizer(InputStream inStream) throws IOException {
      super(inStream);
      this.srcByteBuffer = MemoryUtil.memAlloc(srcBuffSize);
      this.srcTempArray = new byte[srcBuffSize];
      synchronized (this) {
         this.stream = Zstd.ZSTD_createDStream();
         ZstdUtil.checkZstdError(Zstd.ZSTD_DCtx_reset(this.stream, 1));
         ZstdUtil.checkZstdError(Zstd.ZSTD_DCtx_loadDictionary(this.stream, null));
         this.zstdInBuffer = ZSTDInBuffer.malloc();
         this.zstdOutBuffer = ZSTDOutBuffer.malloc();
      }
   }

   public synchronized ZstdInputStreamNoFinalizer setContinuous(boolean b) {
      this.isContinuous = b;
      return this;
   }

   public synchronized boolean getContinuous() {
      return this.isContinuous;
   }

   public synchronized ZstdInputStreamNoFinalizer setLongMax(int windowLogMax) throws IOException {
      ZstdUtil.checkZstdError(Zstd.ZSTD_DCtx_setParameter(this.stream, 100, windowLogMax));
      return this;
   }

   @Override
   public synchronized int read(byte[] dst, int offset, int len) throws IOException {
      if (offset < 0 || len > dst.length - offset) {
         throw new IndexOutOfBoundsException("Requested length " + len + " from offset " + offset + " in buffer of size " + dst.length);
      } else if (len == 0) {
         return 0;
      } else {
         int result = 0;

         while (result == 0) {
            result = this.readInternal(dst, offset, len);
         }

         return result;
      }
   }

   int readInternal(byte[] dst, int offset, int len) throws IOException {
      if (this.isClosed) {
         throw new IOException("Stream closed");
      } else if (offset >= 0 && len <= dst.length - offset) {
         int dstSize = offset + len;
         long dstPos = (long)offset;
         long lastDstPos = -1L;

         while (dstPos < (long)dstSize && lastDstPos < dstPos) {
            if (this.needRead && (this.in.available() > 0 || dstPos == (long)offset)) {
               this.srcSize = (long)this.in.read(this.srcTempArray, 0, srcBuffSize);
               this.srcPos = 0L;
               if (this.srcSize < 0L) {
                  this.srcSize = 0L;
                  if (this.frameFinished) {
                     return -1;
                  }

                  if (this.isContinuous) {
                     this.srcSize = (long)((int)(dstPos - (long)offset));
                     if (this.srcSize > 0L) {
                        return (int)this.srcSize;
                     }

                     return -1;
                  }

                  throw new ZstdIOException(20L, "Truncated source");
               }

               if (this.srcSize == 0L) {
                  continue;
               }

               this.frameFinished = false;
               this.srcByteBuffer.put(0, this.srcTempArray, 0, (int)this.srcSize).rewind();
            }

            lastDstPos = dstPos;
            this.zstdInBuffer.src(this.srcByteBuffer.slice(0, (int)this.srcSize)).pos(this.srcPos);
            ByteBuffer dstByteBuffer = MemoryUtil.memAlloc((int)((long)len - dstPos));

            int size;
            try {
               this.zstdOutBuffer.dst(dstByteBuffer).pos(0L);
               size = (int)ZstdUtil.checkZstdError(Zstd.ZSTD_decompressStream(this.stream, this.zstdOutBuffer, this.zstdInBuffer));
               dstByteBuffer.get(dst, (int)dstPos, (int)this.zstdOutBuffer.pos());
            } finally {
               MemoryUtil.memFree(dstByteBuffer);
            }

            this.srcPos = this.zstdInBuffer.pos();
            dstPos += this.zstdOutBuffer.pos();
            if (size == 0) {
               this.frameFinished = true;
               this.needRead = this.srcPos == this.srcSize;
               return (int)(dstPos - (long)offset);
            }

            this.needRead = dstPos < (long)dstSize;
         }

         return (int)(dstPos - (long)offset);
      } else {
         throw new IndexOutOfBoundsException("Requested length " + len + " from offset " + offset + " in buffer of size " + dst.length);
      }
   }

   @Override
   public synchronized int read() throws IOException {
      byte[] oneByte = new byte[1];
      int result = 0;

      while (result == 0) {
         result = this.readInternal(oneByte, 0, 1);
      }

      return result == 1 ? oneByte[0] & 0xFF : -1;
   }

   @Override
   public synchronized int available() throws IOException {
      if (this.isClosed) {
         throw new IOException("Stream closed");
      } else {
         return !this.needRead ? 1 : this.in.available();
      }
   }

   @Override
   public boolean markSupported() {
      return false;
   }

   @Override
   public synchronized long skip(long numBytes) throws IOException {
      if (this.isClosed) {
         throw new IOException("Stream closed");
      } else if (numBytes <= 0L) {
         return 0L;
      } else {
         int bufferLen = (int)Zstd.ZSTD_DStreamOutSize();
         if ((long)bufferLen > numBytes) {
            bufferLen = (int)numBytes;
         }

         byte[] buf = new byte[bufferLen];
         long toSkip = numBytes;
         byte[] data = buf;

         while (toSkip > 0L) {
            int read = this.read(data, 0, (int)Math.min((long)bufferLen, toSkip));
            if (read < 0) {
               break;
            }

            toSkip -= (long)read;
         }

         return numBytes - toSkip;
      }
   }

   @Override
   public synchronized void close() throws IOException {
      if (!this.isClosed) {
         this.isClosed = true;
         MemoryUtil.memFree(this.srcByteBuffer);
         Zstd.ZSTD_freeDStream(this.stream);
         this.zstdInBuffer.close();
         this.zstdOutBuffer.close();
         this.in.close();
      }
   }
}
