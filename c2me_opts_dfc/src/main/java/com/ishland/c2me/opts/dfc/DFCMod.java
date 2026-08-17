package com.ishland.c2me.opts.dfc;

import net.minecraftforge.fml.common.Mod;

@Mod("c2me_opts_dfc")
public class DFCMod {

    public DFCMod() {
        // ensure ModuleEntryPoint is initialized (registers config options)
        new ModuleEntryPoint();
    }
}
