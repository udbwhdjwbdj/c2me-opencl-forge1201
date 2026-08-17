package com.ishland.c2me.opts.dfc.common;

import com.ishland.c2me.base.common.config.ConfigSystem.ConfigAccessor;

public class Config {
   public static final boolean enableBuiltinIntegrations = new ConfigAccessor()
      .key("vanillaWorldGenOptimizations.enableBuiltinDFCIntegrations")
      .comment(
         "Enables the built-in integrations with other worldgen mods in density function compiler.\n\nDepends on vanillaWorldGenOptimizations.useDensityFunctionCompiler\n"
      )
      .getBoolean(true, false);

   public static void init() {
   }

   private Config() {
   }
}
