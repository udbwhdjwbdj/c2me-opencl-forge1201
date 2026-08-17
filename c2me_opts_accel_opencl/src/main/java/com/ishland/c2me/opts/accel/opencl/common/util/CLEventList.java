package com.ishland.c2me.opts.accel.opencl.common.util;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;

public class CLEventList extends LongArrayList {
   public PointerBuffer getEventWaitList(MemoryStack stack) {
      int size = this.size();
      PointerBuffer buffer = stack.mallocPointer(size);

      for (int i = 0; i < size; i++) {
         buffer.put(i, this.getLong(i));
      }

      return (PointerBuffer)buffer.rewind();
   }
}
