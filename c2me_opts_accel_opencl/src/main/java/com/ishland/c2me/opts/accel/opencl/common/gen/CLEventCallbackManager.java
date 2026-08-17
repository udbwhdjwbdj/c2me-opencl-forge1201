package com.ishland.c2me.opts.accel.opencl.common.gen;

import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import java.io.Closeable;
import org.lwjgl.opencl.CL12;
import org.lwjgl.opencl.CLEventCallback;
import org.lwjgl.opencl.CLEventCallbackI;
import org.lwjgl.system.NativeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CLEventCallbackManager implements Closeable {
   private static final Logger LOGGER = LoggerFactory.getLogger(CLEventCallbackManager.class);
   private final Long2ReferenceOpenHashMap<CLEventCallbackI> callbacks = new Long2ReferenceOpenHashMap();
   private final CLEventCallback instance = CLEventCallback.create(this::invokeInternal);
   private long ordinal = 0L;
   private boolean open = true;

   public void registerCallback(
      @NativeType("cl_event") long event,
      @NativeType("cl_int") int command_exec_callback_type,
      @NativeType("void (*) (cl_event, cl_int, void *)") CLEventCallbackI pfn_notify
   ) {
      long user_data;
      synchronized (this) {
         if (!this.open) {
            throw new IllegalStateException("CLEventCallbackManager is closed");
         }

         user_data = this.ordinal++;
         this.callbacks.put(user_data, pfn_notify);
      }

      CL12.clSetEventCallback(event, command_exec_callback_type, this.instance, user_data);
   }

   private void invokeInternal(@NativeType("cl_event") long event, @NativeType("cl_int") int event_command_exec_status, @NativeType("void *") long user_data) {
      CLEventCallbackI callbackI;
      synchronized (this) {
         callbackI = (CLEventCallbackI)this.callbacks.remove(user_data);
         if (!this.open && callbackI != null && this.callbacks.isEmpty()) {
            this.instance.close();
         }
      }

      if (callbackI == null) {
         LOGGER.error("Dangling callback ID {}, invoked by driver twice?", user_data);
      } else {
         try {
            callbackI.invoke(event, event_command_exec_status, user_data);
         } catch (Throwable var9) {
            LOGGER.error("Error in callback ID {}", user_data, var9);
         }
      }
   }

   @Override
   public void close() {
      synchronized (this) {
         if (this.open && this.callbacks.isEmpty()) {
            this.instance.close();
         }

         this.open = false;
      }
   }
}
