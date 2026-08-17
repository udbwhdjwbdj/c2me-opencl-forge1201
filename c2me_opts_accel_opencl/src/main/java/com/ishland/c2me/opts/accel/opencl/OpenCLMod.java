package com.ishland.c2me.opts.accel.opencl;

import net.minecraftforge.fml.common.Mod;

@Mod("c2me_opts_accel_opencl")
public class OpenCLMod {

    public OpenCLMod() {
        // ensure ModuleEntryPoint is initialized (Config.init())
        new ModuleEntryPoint();
    }
}
