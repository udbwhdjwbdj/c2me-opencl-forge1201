package com.ishland.c2me.opts.accel.opencl.common.util;

import com.ishland.c2me.opts.accel.opencl.common.gen.cache.Stage1Cache;

public class TLUtil {
   public static final ThreadLocal<Stage1Cache.AreaCacheEntry> stage1CachePassing = new ThreadLocal<>();
}
