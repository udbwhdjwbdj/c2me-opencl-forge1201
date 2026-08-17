package com.ishland.c2me.opts.accel.opencl.common.gen;

import com.google.common.collect.ImmutableList;
import com.ishland.c2me.opts.accel.opencl.common.enumeration.OpenCLDeviceMetadata;
import com.ishland.c2me.opts.accel.opencl.common.shader_cache.ShaderCacheManager;
import com.ishland.c2me.opts.accel.opencl.common.util.CLUtil;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import org.lwjgl.opencl.CL12;

public class CLServerGlobalContext {
   private final ArrayList<OpenCLDevice> openDevices = new ArrayList<>();
   private final AtomicInteger currentDeviceIndex = new AtomicInteger(0);
   private final ReferenceArrayList<CLServerWorldContext> registeredWorlds = new ReferenceArrayList();
   final ShaderCacheManager shaderCacheManager = new ShaderCacheManager();
   final ReentrantLock takeLock = new ReentrantLock();
   final Condition notEmpty = this.takeLock.newCondition();

   public boolean openDevice(OpenCLDeviceMetadata device) {
      OpenCLDevice openCLDevice;
      synchronized (this) {
         for (OpenCLDevice openDevice : this.openDevices) {
            if (openDevice.getMetadata().deviceUUID.equals(device.deviceUUID)) {
               return false;
            }
         }

         openCLDevice = new OpenCLDevice(this, device);
         this.openDevices.add(openCLDevice);
      }

      ObjectListIterator var3 = this.registeredWorlds.iterator();

      while (var3.hasNext()) {
         CLServerWorldContext world = (CLServerWorldContext)var3.next();
         world.addDevice(openCLDevice);
      }

      return true;
   }

   public void closeDevice(OpenCLDeviceMetadata device) {
      OpenCLDevice openCLDevice = null;
      synchronized (this) {
         for (OpenCLDevice openDevice : this.openDevices) {
            if (openDevice.getMetadata().deviceUUID.equals(device.deviceUUID)) {
               openCLDevice = openDevice;
               this.openDevices.remove(openDevice);
               break;
            }
         }
      }

      this.closeDevice0(openCLDevice);
   }

   private void closeDevice0(OpenCLDevice openCLDevice) {
      if (openCLDevice != null) {
         ObjectListIterator var2 = this.registeredWorlds.iterator();

         while (var2.hasNext()) {
            CLServerWorldContext world = (CLServerWorldContext)var2.next();
            world.removeDevice(openCLDevice);
         }

         openCLDevice.close();
      }
   }

   public synchronized void closeAllDevices() {
      for (OpenCLDevice openDevice : this.openDevices) {
         try {
            this.closeDevice0(openDevice);
            CLUtil.checkCLError(CL12.clUnloadPlatformCompiler(openDevice.getMetadata().platformPtr));
         } catch (Throwable var4) {
            var4.printStackTrace();
         }
      }

      this.openDevices.clear();
      CL12.clUnloadCompiler();
   }

   void registerWorld(CLServerWorldContext world) {
      this.registeredWorlds.add(world);

      for (OpenCLDevice openDevice : this.openDevices) {
         world.addDevice(openDevice);
      }
   }

   void unregisterWorld(CLServerWorldContext world) {
      this.registeredWorlds.remove(world);

      for (OpenCLDevice openDevice : this.openDevices) {
         world.removeDevice(openDevice);
      }
   }

   void signalNotEmpty() {
      this.takeLock.lock();

      try {
         this.notEmpty.signal();
      } finally {
         this.takeLock.unlock();
      }
   }

   public List<OpenCLDevice> getOpenDevices() {
      return ImmutableList.copyOf(this.openDevices);
   }
}
