package com.ishland.c2me.opts.accel.opencl.mixin;

import com.ishland.c2me.opts.accel.opencl.common.Config;
import com.ishland.c2me.opts.accel.opencl.common.ducks.MinecraftServerExtension;
import com.ishland.c2me.opts.accel.opencl.common.enumeration.OpenCLDeviceLocator;
import com.ishland.c2me.opts.accel.opencl.common.enumeration.OpenCLDeviceMetadata;
import com.ishland.c2me.opts.accel.opencl.common.gen.CLServerGlobalContext;
import com.ishland.c2me.opts.accel.opencl.common.progress.GlobalProgressStash;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({MinecraftServer.class})
public class MixinMinecraftServer implements MinecraftServerExtension {
   @Shadow
   @Final
   private static Logger LOGGER;
   @Unique
   private CLServerGlobalContext c2me$clContext;

   @Inject(
      method = {"runServer"},
      at = {@At("HEAD")}
   )
   private void preRunServer(CallbackInfo ci) {
      try {
         if (this.c2me$clContext != null) {
            throw new IllegalStateException("Context already exists?");
         }

         this.c2me$clContext = new CLServerGlobalContext();
         List<OpenCLDeviceMetadata> metadataList = OpenCLDeviceLocator.enumerateAll();
         boolean openedAnyDevice = false;

         for (OpenCLDeviceMetadata openCLDeviceMetadata : metadataList) {
            this.c2me$clContext.openDevice(openCLDeviceMetadata);
            openedAnyDevice = true;
         }

         if (!openedAnyDevice) {
            LOGGER.warn("No OpenCL devices found");
            if (!Config.allowIncompatibilityFallback) {
               throw new IllegalStateException("No OpenCL devices found");
            }

            this.c2me$clContext = null;
            return;
         }
      } catch (Throwable var6) {
         LOGGER.error("Failed to initialize OpenCL context", var6);
         this.c2me$clContext = null;
         if (!Config.allowIncompatibilityFallback) {
            GlobalProgressStash.PROGRESS_TEXT = String.format("Failed to initialize OpenCL context, see logs for details: %s", var6);
            throw var6;
         }
      }
   }

   @Inject(
      method = {"stopServer"},
      at = {@At("RETURN")}
   )
   private void postStopServer(CallbackInfo ci) {
      try {
         if (this.c2me$clContext != null) {
            this.c2me$clContext.closeAllDevices();
            this.c2me$clContext = null;
         }
      } catch (Throwable var3) {
         LOGGER.error("Failed to release OpenCL context", var3);
      }
   }

   @Override
   public CLServerGlobalContext c2me$getCLContext() {
      return this.c2me$clContext;
   }
}
