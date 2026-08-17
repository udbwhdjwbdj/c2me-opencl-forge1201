package com.ishland.c2me.opts.accel.opencl.common.util;

import net.minecraft.world.level.levelgen.Beardifier;

public class BeardifierTables {

    private static volatile float[] structureWeightTable;

    public static float[] getStructureWeightTable() {
        float[] table = structureWeightTable;
        if (table == null) {
            synchronized (BeardifierTables.class) {
                table = structureWeightTable;
                if (table == null) {
                    table = Beardifier.BEARD_KERNEL;
                    structureWeightTable = table;
                }
            }
        }
        return table;
    }
}
