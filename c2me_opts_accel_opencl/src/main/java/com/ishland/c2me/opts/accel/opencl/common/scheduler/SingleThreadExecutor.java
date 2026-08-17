package com.ishland.c2me.opts.accel.opencl.common.scheduler;

import com.google.common.base.Preconditions;
import io.netty.util.internal.PlatformDependent;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.NotNull;

public class SingleThreadExecutor extends Thread implements Executor {
   private final AtomicInteger size = new AtomicInteger();
   public final Queue<Runnable> queue = PlatformDependent.newMpscQueue();
   private final AtomicBoolean shutdown = new AtomicBoolean(false);
   private final Object sync = new Object();

   @Override
   public void run() {
      while (true) {
         if (!this.pollTasks()) {
            if (this.shutdown.get()) {
               return;
            }

            synchronized (this.sync) {
               if (this.size.get() == 0 && !this.shutdown.get()) {
                  try {
                     this.sync.wait();
                  } catch (InterruptedException var4) {
                  }
               }
            }
         }
      }
   }

   private boolean pollTasks() {
      boolean hasWork;
      Runnable task;
      for (hasWork = false; (task = this.queue.poll()) != null; hasWork = true) {
         this.size.decrementAndGet();

         try {
            task.run();
         } catch (Throwable var4) {
            var4.printStackTrace();
         }
      }

      return hasWork;
   }

   @Override
   public void execute(@NotNull Runnable command) {
      Preconditions.checkNotNull(command, "command");
      boolean wasEmpty = this.size.getAndIncrement() == 0;
      this.queue.add(command);
      if (wasEmpty) {
         synchronized (this.sync) {
            this.sync.notify();
         }
      }
   }

   public void shutdown() {
      this.shutdown.set(true);
      synchronized (this.sync) {
         this.sync.notify();
      }
   }
}
