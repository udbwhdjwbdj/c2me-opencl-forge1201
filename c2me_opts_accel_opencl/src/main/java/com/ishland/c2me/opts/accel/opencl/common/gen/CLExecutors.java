package com.ishland.c2me.opts.accel.opencl.common.gen;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CLExecutors {
   public static final ThreadPoolExecutor COMPILE_POOL = new ThreadPoolExecutor(
      1, 1, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new ThreadFactoryBuilder().setDaemon(true).setPriority(1).setNameFormat("c2me-clc-%d").build()
   );

   static {
      COMPILE_POOL.allowCoreThreadTimeOut(true);
   }
}
