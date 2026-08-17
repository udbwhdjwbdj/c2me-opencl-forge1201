package com.ishland.c2me.opts.accel.opencl.common.gen.cache;

import com.ishland.c2me.opts.accel.opencl.common.util.MemoryUtil;
import com.ishland.c2me.opts.accel.opencl.common.Config;
import com.ishland.c2me.opts.accel.opencl.common.util.CLUtil;
import com.ishland.c2me.opts.accel.opencl.common.util.FlowschedAssertions;
import it.unimi.dsi.fastutil.longs.Long2LongFunction;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongListIterator;
import java.util.Arrays;
import java.util.Objects;
import org.lwjgl.opencl.CL12;

public class CLBufferCache {
   private static final int MAX_CACHE_SIZE = Config.maxConcurrentTasksPerDevice + 4;
   private final int[] cacheBufferSizes = new int[CLBufferCache.Type.values().length];
   private final LongArrayList[] caches = new LongArrayList[CLBufferCache.Type.values().length];
   private final Thread owner;

   public CLBufferCache(Thread owner) {
      for (int i = 0; i < this.caches.length; i++) {
         this.caches[i] = new LongArrayList();
      }

      this.owner = Objects.requireNonNull(owner);
   }

   public synchronized CLBufferCache.BufferEntry allocate(CLBufferCache.Type type, int size, Long2LongFunction allocator) {
      FlowschedAssertions.assertTrue(
         Thread.currentThread() == this.owner,
         "Buffer cache can only be accessed from its owner thread (Current: %s, Owner: %s)",
         new Object[]{Thread.currentThread(), this.owner}
      );
      int cacheBufferSize = this.cacheBufferSizes[type.ordinal()];
      if (cacheBufferSize < size) {
         this.cacheBufferSizes[type.ordinal()] = cacheBufferSize = MemoryUtil.roundUp(size, 4096);
         clearCache0(this.caches[type.ordinal()]);
      }

      long l = this.tryBorrow0(type);
      if (l == 0L) {
         l = allocator.get((long)cacheBufferSize);
      }

      return new CLBufferCache.BufferEntry(l, cacheBufferSize);
   }

   private synchronized long tryBorrow0(CLBufferCache.Type type) {
      FlowschedAssertions.assertTrue(
         Thread.currentThread() == this.owner,
         "Buffer cache can only be accessed from its owner thread (Current: %s, Owner: %s)",
         new Object[]{Thread.currentThread(), this.owner}
      );
      LongArrayList cache = this.caches[type.ordinal()];
      return cache != null && !cache.isEmpty() ? cache.removeLong(cache.size() - 1) : 0L;
   }

   public void returnBuffer(CLBufferCache.Type type, CLBufferCache.BufferEntry entry) {
      this.returnBuffer(type, entry.size(), entry.buffer());
   }

   public synchronized void returnBuffer(CLBufferCache.Type type, int size, long buffer) {
      FlowschedAssertions.assertTrue(
         Thread.currentThread() == this.owner,
         "Buffer cache can only be accessed from its owner thread (Current: %s, Owner: %s)",
         new Object[]{Thread.currentThread(), this.owner}
      );
      LongArrayList cache = this.caches[type.ordinal()];
      if (cache != null && this.cacheBufferSizes[type.ordinal()] == size) {
         cache.add(buffer);

         while (cache.size() > MAX_CACHE_SIZE) {
            long removed = cache.removeLong(0);

            try {
               CLUtil.checkCLError(CL12.clReleaseMemObject(removed));
            } catch (Throwable var9) {
               var9.printStackTrace();
            }
         }
      } else {
         try {
            CLUtil.checkCLError(CL12.clReleaseMemObject(buffer));
         } catch (Throwable var10) {
            var10.printStackTrace();
         }
      }
   }

   public synchronized void close() {
      for (LongArrayList cache : this.caches) {
         clearCache0(cache);
      }

      Arrays.fill(this.caches, null);
   }

   private static void clearCache0(LongArrayList cache) {
      LongListIterator var1 = cache.iterator();

      while (var1.hasNext()) {
         long buffer = (Long)var1.next();

         try {
            CLUtil.checkCLError(CL12.clReleaseMemObject(buffer));
         } catch (Throwable var5) {
            var5.printStackTrace();
         }
      }

      cache.clear();
   }

   public static record BufferEntry(long buffer, int size) {
   }

   public static enum Type {
      ESTIMATE_SURFACE_HEIGHT_RX,
      FLATCACHE_RX,
      GEN_STAGE1_RW_DATA,
      GEN_RW_DATA,
      GEN_BLOCK_STATE_RX,
      GEN_BIOME_RX,
      GEN_BIOME_RW_DATA,
      GEN_BATCHING_RW_DATA,
      GEN_BATCHING_BLOCK_STATE_RX,
      GEN_BATCHING_BIOME_RX;
   }
}
