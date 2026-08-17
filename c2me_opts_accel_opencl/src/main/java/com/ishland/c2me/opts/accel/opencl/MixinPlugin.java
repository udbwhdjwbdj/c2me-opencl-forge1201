package com.ishland.c2me.opts.accel.opencl;

import com.ishland.c2me.base.common.ModuleMixinPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MixinPlugin extends ModuleMixinPlugin {
   private static final Logger LOGGER = LoggerFactory.getLogger(MixinPlugin.class);
   private static final boolean DISABLE_FEATURES_SURFACE_RULES = Boolean.getBoolean("c2me.opencl.debug.disable_features_surface_rules");

   public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
      if (!super.shouldApplyMixin(targetClassName, mixinClassName)) {
         return false;
      } else {
         return mixinClassName.startsWith("com.ishland.c2me.opts.accel.opencl.mixin.debug.disable_features_surface_rules.")
            ? DISABLE_FEATURES_SURFACE_RULES
            : true;
      }
   }

   static {
      if (DISABLE_FEATURES_SURFACE_RULES) {
         LOGGER.warn("c2me.opencl.debug.disable_features_surface_rules is enabled, features and surface rules will be disabled!");
      }
   }
}
