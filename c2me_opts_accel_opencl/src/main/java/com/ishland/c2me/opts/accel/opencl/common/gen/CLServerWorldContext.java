package com.ishland.c2me.opts.accel.opencl.common.gen;

import com.ishland.c2me.opts.accel.opencl.common.compiler.GeneratedCLSource;
import com.ishland.c2me.opts.accel.opencl.common.compiler.OpenCLCGen;
import com.ishland.c2me.opts.accel.opencl.common.compiler.emitters.misc.CLBlockStateMappings;
import com.ishland.c2me.opts.accel.opencl.common.gen.cache.Stage1Cache;
import com.ishland.c2me.opts.accel.opencl.common.util.CLUtil;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import net.minecraft.world.level.levelgen.NoiseSettings;
import org.lwjgl.opencl.CL12;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CLServerWorldContext {
   private static final Logger LOGGER = LoggerFactory.getLogger(CLServerWorldContext.class);
   private final ArrayList<CLServerWorldContext.DeviceWithProgram> openDevices = new ArrayList<>();
   private final ReferenceOpenHashSet<Pair<OpenCLDevice, CompletableFuture<Void>>> pendingCompilations = new ReferenceOpenHashSet();
   private final Stage1Cache stage1Cache;
   private final CLServerGlobalContext globalContext;
   private final String description;
   private final GeneratedCLSource generatedCLSource;
   private final NoiseSettings generationShapeConfig;
   private final CLBlockStateMappings clBlockStateMappings;

   public CLServerWorldContext(
      CLServerGlobalContext globalContext,
      String description,
      GeneratedCLSource generatedCLSource,
      NoiseSettings generationShapeConfig,
      CLBlockStateMappings clBlockStateMappings
   ) {
      this.globalContext = globalContext;
      this.description = description;
      this.generatedCLSource = Objects.requireNonNull(generatedCLSource);
      this.generationShapeConfig = Objects.requireNonNull(generationShapeConfig);
      this.clBlockStateMappings = Objects.requireNonNull(clBlockStateMappings);
      this.stage1Cache = new Stage1Cache(this);
      this.globalContext.registerWorld(this);
   }

   public void addDevice(OpenCLDevice device) {
      synchronized (this.pendingCompilations) {
         ObjectIterator future = this.pendingCompilations.iterator();

         while (future.hasNext()) {
            Pair<OpenCLDevice, CompletableFuture<Void>> pendingCompilation = (Pair<OpenCLDevice, CompletableFuture<Void>>)future.next();
            if (pendingCompilation.left() == device) {
               return;
            }
         }

         LOGGER.info("Compiling program for {} for device {}", this.description, device);
         CompletableFuture<Void> futurex = device.compileProgramAsync(this.description, this.generatedCLSource).thenAccept(program -> {
            ByteBuffer byteBuffer = MemoryUtil.memAlloc(this.generatedCLSource.getConstData().length);

            long clBuffer;
            try {
               MemoryStack stack = MemoryStack.stackPush();

               try {
                  byteBuffer.put(this.generatedCLSource.getConstData());
                  byteBuffer.rewind();
                  IntBuffer errorCodeRet = stack.callocInt(1);
                  clBuffer = CL12.clCreateBuffer(device.getContext(), 36L, byteBuffer, errorCodeRet);
               } catch (Throwable var17) {
                  if (stack != null) {
                     try {
                        stack.close();
                     } catch (Throwable var15) {
                        var17.addSuppressed(var15);
                     }
                  }

                  throw var17;
               }

               if (stack != null) {
                  stack.close();
               }
            } finally {
               MemoryUtil.memFree(byteBuffer);
            }

            synchronized (this.openDevices) {
               this.openDevices.add(new CLServerWorldContext.DeviceWithProgram(device, (EnumMap<OpenCLCGen.ProgramType, Long>)program, clBuffer));
            }

            LOGGER.info("Compiled program for {} for device {}", this.description, device);
         }).exceptionally(throwable -> {
            LOGGER.error("Failed to compile program for device {}", device, throwable);
            return null;
         });
         Pair<OpenCLDevice, CompletableFuture<Void>> pair = Pair.of(device, futurex);
         this.pendingCompilations.add(pair);
         futurex.handle((var3x, var4x) -> {
            try {
               boolean releaseProgram = false;
               synchronized (this.pendingCompilations) {
                  if (!this.pendingCompilations.remove(pair)) {
                     releaseProgram = true;
                  }
               }

               this.globalContext.signalNotEmpty();
               if (releaseProgram) {
                  this.removeDevice(device);
               }
            } catch (Throwable var9) {
               LOGGER.error("Failed to remove pending compilation", var9);
            }

            return null;
         });
      }
   }

   public void removeDevice(OpenCLDevice device) {
      synchronized (this.pendingCompilations) {
         ObjectIterator var3 = this.pendingCompilations.iterator();

         while (var3.hasNext()) {
            Pair<OpenCLDevice, CompletableFuture<Void>> pendingCompilation = (Pair<OpenCLDevice, CompletableFuture<Void>>)var3.next();
            if (pendingCompilation.left() == device) {
               this.pendingCompilations.remove(pendingCompilation);
               return;
            }
         }
      }

      synchronized (this.openDevices) {
         for (CLServerWorldContext.DeviceWithProgram deviceWithProgram : this.openDevices) {
            if (deviceWithProgram.device() == device) {
               this.removeDevice0(deviceWithProgram);
               this.openDevices.remove(deviceWithProgram);
               return;
            }
         }
      }
   }

   private void removeDevice0(CLServerWorldContext.DeviceWithProgram deviceWithProgram) {
      for (long program : deviceWithProgram.program()) {
         if (program != 0L) {
            try {
               CLUtil.checkCLError(CL12.clReleaseProgram(program));
            } catch (Throwable var9) {
               LOGGER.error("Failed to release program for device {}", deviceWithProgram.device(), var9);
            }
         }
      }

      try {
         CLUtil.checkCLError(CL12.clReleaseMemObject(deviceWithProgram.programConstDataBuffer()));
      } catch (Throwable var8) {
         LOGGER.error("Failed to release buffer for device {}", deviceWithProgram.device(), var8);
      }

      LOGGER.info("Released program for {} for device {}", this.description, deviceWithProgram.device());
   }

   public Pair<OpenCLDevice.BorrowedCommandQueue, CLServerWorldContext.DeviceWithProgram> tryBorrowCommandQueue() {
      synchronized (this.openDevices) {
         int size = this.openDevices.size();
         if (size == 0) {
            return null;
         } else {
            CLServerWorldContext.DeviceWithProgram leastTask = (CLServerWorldContext.DeviceWithProgram)this.openDevices.get(0);

            for (int i = 1; i < size; i++) {
               CLServerWorldContext.DeviceWithProgram current = this.openDevices.get(i);
               if (current.device.getPermits() > leastTask.device.getPermits()) {
                  leastTask = current;
               }
            }

            OpenCLDevice.BorrowedCommandQueue borrowed = leastTask.device().borrowCommandQueue();
            return borrowed != null ? Pair.of(borrowed, leastTask) : null;
         }
      }
   }

   public CompletableFuture<Pair<OpenCLDevice.BorrowedCommandQueue, CLServerWorldContext.DeviceWithProgram>> borrowCommandQueue() {
      CompletableFuture<Pair<OpenCLDevice.BorrowedCommandQueue, CLServerWorldContext.DeviceWithProgram>> future = new CompletableFuture<>();
      Thread borrowThread = new Thread(() -> {
         try {
            this.globalContext.takeLock.lock();

            try {
               while (true) {
                  Pair<OpenCLDevice.BorrowedCommandQueue, CLServerWorldContext.DeviceWithProgram> borrowed = this.tryBorrowCommandQueue();
                  if (borrowed != null) {
                     future.complete(borrowed);
                     return;
                  }

                  this.globalContext.notEmpty.await();
               }
            } finally {
               this.globalContext.takeLock.unlock();
            }
         } catch (Throwable var7) {
            future.completeExceptionally(var7);
         }
      });
      borrowThread.setDaemon(true);
      borrowThread.start();
      return future;
   }

   public void releaseAllDevices() {
      synchronized (this.pendingCompilations) {
         this.pendingCompilations.clear();
      }

      synchronized (this.openDevices) {
         for (CLServerWorldContext.DeviceWithProgram deviceWithProgram : this.openDevices) {
            this.removeDevice0(deviceWithProgram);
         }

         this.openDevices.clear();
      }

      this.globalContext.unregisterWorld(this);
   }

   public NoiseSettings getGenerationShapeConfig() {
      return this.generationShapeConfig;
   }

   public Stage1Cache getEstimateSurfaceHeightCache() {
      return this.stage1Cache;
   }

   public GeneratedCLSource getGeneratedCLSource() {
      return this.generatedCLSource;
   }

   public CLBlockStateMappings getClBlockStateMappings() {
      return this.clBlockStateMappings;
   }

   public static record DeviceWithProgram(OpenCLDevice device, long[] program, long programConstDataBuffer) {
      public DeviceWithProgram(OpenCLDevice device, EnumMap<OpenCLCGen.ProgramType, Long> program, long programConstDataBuffer) {
         this(device, enumMap2Array(program), programConstDataBuffer);
      }

      public long getProgram(OpenCLCGen.ProgramType type) {
         long l = this.program[type.ordinal()];
         if (l == 0L) {
            throw new IllegalStateException("Program for type " + type + " is not available");
         } else {
            return l;
         }
      }

      private static long[] enumMap2Array(EnumMap<OpenCLCGen.ProgramType, Long> program) {
         long[] array = new long[OpenCLCGen.ProgramType.values().length];

         for (OpenCLCGen.ProgramType type : OpenCLCGen.ProgramType.values()) {
            array[type.ordinal()] = program.getOrDefault(type, Long.valueOf(0L));
         }

         return array;
      }
   }
}
