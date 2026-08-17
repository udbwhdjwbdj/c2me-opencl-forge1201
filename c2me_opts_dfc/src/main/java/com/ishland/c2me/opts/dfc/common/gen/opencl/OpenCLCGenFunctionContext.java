package com.ishland.c2me.opts.dfc.common.gen.opencl;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.gen.meta.ValuesMethodDef;
import com.ishland.c2me.opts.dfc.common.gen.meta.ValuesMethodDefF32;
import com.ishland.c2me.opts.dfc.common.gen.meta.ValuesMethodDefF64;

public interface OpenCLCGenFunctionContext {
   OpenCLCGenContext getGlobalContext();

   OpenCLCGenFunctionContext.FunctionVariant getVariant();

   String nextVarName();

   ValuesMethodDef newVar(AstNode var1);

   ValuesMethodDefF64 newVarF64(AstNode var1);

   ValuesMethodDefF32 newVarF32(AstNode var1);

   String newVarUnoptimized(AstNode var1);

   String getDelegateVar(ValuesMethodDef var1, AstNode.ReturnType var2);

   String getDelegateVar(ValuesMethodDefF64 var1);

   String getDelegateVar(ValuesMethodDefF32 var1);

   OpenCLCGenFunctionContext fork();

   String getBody();

   void appendRaw(String var1);

   public static enum FunctionVariant {
      UNCACHED("_uncached", true, false, false, false),
      FLATCACHE_ONLY("_flatcache_only", true, true, false, false),
      FULLY_CACHED("_fully_cached", true, true, true, false),
      FULLY_CACHED_EXCEPT_CACHE2D("_fully_cached_except_cache2d", false, true, true, false);

      public final String suffix;
      public final boolean inDispatcher;
      public final boolean enableFlatCache;
      public final boolean enableAllCache;
      private final boolean disableCache2d;

      private FunctionVariant(String suffix, boolean inDispatcher, boolean enableFlatCache, boolean enableAllCache, boolean disableCache2d) {
         this.suffix = suffix;
         this.inDispatcher = inDispatcher;
         this.enableFlatCache = enableFlatCache;
         this.enableAllCache = enableAllCache;
         this.disableCache2d = disableCache2d;
      }

      public boolean useCache2D() {
         return this.enableAllCache && !this.disableCache2d;
      }
   }
}
