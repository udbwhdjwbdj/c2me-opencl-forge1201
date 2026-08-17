package com.ishland.c2me.opts.accel.opencl.common.util;

/**
 * Minimal 2D array cache replacement for the 1.20.2+ vanilla StaticCache2D,
 * used by the OpenCL worldgen integration.
 */
public class StaticCache2D<T> {

    public interface Initializer<T> {
        T get(int x, int z);
    }

    private final int startX;
    private final int startZ;
    private final int sizeX;
    private final int sizeZ;
    private final Object[] data;

    public StaticCache2D(int startX, int startZ, int sizeX, int sizeZ, Initializer<T> initializer) {
        this.startX = startX;
        this.startZ = startZ;
        this.sizeX = sizeX;
        this.sizeZ = sizeZ;
        this.data = new Object[sizeX * sizeZ];
        for (int x = 0; x < sizeX; x++) {
            for (int z = 0; z < sizeZ; z++) {
                this.data[x * sizeZ + z] = initializer.get(startX + x, startZ + z);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public T get(int x, int z) {
        return (T) this.data[(x - this.startX) * this.sizeZ + (z - this.startZ)];
    }
}
