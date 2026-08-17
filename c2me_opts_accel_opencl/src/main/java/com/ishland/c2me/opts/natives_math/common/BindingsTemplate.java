package com.ishland.c2me.opts.natives_math.common;

import com.ishland.c2me.opts.accel.opencl.common.util.MemoryUtil;
import com.ishland.c2me.opts.accel.opencl.mixin.access.IInterpolatedNoiseSampler;
import com.ishland.c2me.opts.accel.opencl.mixin.access.IOctavePerlinNoiseSampler;
import com.ishland.c2me.opts.accel.opencl.mixin.access.IPerlinNoiseSampler;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate.Parameter;
import net.minecraft.world.level.biome.Climate.RTree;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;

/**
 * 1.20.1 Forge port of the natives-math serialization helpers.
 * Produces the exact offset-based binary layouts consumed by the OpenCL kernels
 * (double_octave_sampler_data_t / interpolated_noise_sampler_t / biome_search_tree_node_t),
 * matching the NeoForge 0.4.0-alpha.0.120 implementation, without java.lang.foreign.
 */
public class BindingsTemplate {

    // double_octave_sampler_data_t (offset layout, aligned to 64)
    public static final int double_octave_sampler_data_SIZE = 64;
    public static final int double_octave_sampler_data$length = 0;
    public static final int double_octave_sampler_data$amplitude = 8;
    public static final int double_octave_sampler_data$need_shift = 16;
    public static final int double_octave_sampler_data$lacunarity_powd = 20;
    public static final int double_octave_sampler_data$persistence_powd = 24;
    public static final int double_octave_sampler_data$sampler_permutations = 28;
    public static final int double_octave_sampler_data$sampler_originX = 32;
    public static final int double_octave_sampler_data$sampler_originY = 36;
    public static final int double_octave_sampler_data$sampler_originZ = 40;
    public static final int double_octave_sampler_data$amplitudes = 44;

    public static final int interpolated_noise_sub_sampler$length = 0;
    public static final int interpolated_noise_sub_sampler$sampler_permutations = 4;
    public static final int interpolated_noise_sub_sampler$sampler_originX = 8;
    public static final int interpolated_noise_sub_sampler$sampler_originY = 12;
    public static final int interpolated_noise_sub_sampler$sampler_originZ = 16;
    public static final int interpolated_noise_sub_sampler$sampler_mulFactor = 20;
    public static final int interpolated_noise_sub_sampler_SIZE = 24;

    public static final int interpolated_noise_sampler$scaledXzScale = 0;
    public static final int interpolated_noise_sampler$scaledYScale = 8;
    public static final int interpolated_noise_sampler$xzFactor = 16;
    public static final int interpolated_noise_sampler$yFactor = 24;
    public static final int interpolated_noise_sampler$smearScaleMultiplier = 32;
    public static final int interpolated_noise_sampler$xzScale = 40;
    public static final int interpolated_noise_sampler$yScale = 48;
    public static final int interpolated_noise_sampler$lower = 56;
    public static final int interpolated_noise_sampler$upper = 80;
    public static final int interpolated_noise_sampler$normal = 104;
    public static final int interpolated_noise_sampler_SIZE = 128;

    public static final int biome_search_tree_node$state = 0;
    public static final int biome_search_tree_node$children_offset = 4;
    public static final int biome_search_tree_node$maxs = 4;
    public static final int biome_search_tree_node$mins = 18;
    public static final int biome_search_tree_node_SIZE = 32;

    private static ByteBuffer alloc(long size, long alignment) {
        long aligned = MemoryUtil.roundUp(size, alignment);
        return org.lwjgl.system.MemoryUtil.memAlignedAlloc((int) alignment, (int) aligned);
    }

    private static Object reflectField(Object o, String name) {
        try {
            java.lang.reflect.Field f = o.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(o);
        } catch (Throwable t) {
            throw new IllegalStateException("Cannot reflect field " + name + " on " + o.getClass().getName(), t);
        }
    }

    private static Object reflectFieldIfPresent(Object o, String name) {
        try {
            java.lang.reflect.Field f = o.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(o);
        } catch (Throwable t) {
            return null;
        }
    }

    private static void putInt(ByteBuffer b, int off, int v) { b.putInt(off, v); }
    private static void putLong(ByteBuffer b, int off, long v) { b.putLong(off, v); }
    private static void putDouble(ByteBuffer b, int off, double v) { b.putDouble(off, v); }

    public static ByteBuffer double_octave_sampler_data$create(
        PerlinNoise firstSampler, PerlinNoise secondSampler, double amplitude, boolean int8
    ) {
        long nonNullSamplerCount = 0;
        for (ImprovedNoise sampler : ((IOctavePerlinNoiseSampler) firstSampler).getOctaveSamplers()) {
            if (sampler != null) nonNullSamplerCount++;
        }
        for (ImprovedNoise sampler : ((IOctavePerlinNoiseSampler) secondSampler).getOctaveSamplers()) {
            if (sampler != null) nonNullSamplerCount++;
        }
        long need_shift_offset = double_octave_sampler_data_SIZE;
        long lacunarity_powd_offset = MemoryUtil.roundUp(need_shift_offset + nonNullSamplerCount, 64);
        long persistence_powd_offset = MemoryUtil.roundUp(lacunarity_powd_offset + nonNullSamplerCount * 8, 64);
        long sampler_permutations_offset = MemoryUtil.roundUp(persistence_powd_offset + nonNullSamplerCount * 8, 64);
        long sampler_originX_offset = MemoryUtil.roundUp(sampler_permutations_offset + nonNullSamplerCount * 256 * (int8 ? 1 : 4), 64);
        long sampler_originY_offset = MemoryUtil.roundUp(sampler_originX_offset + nonNullSamplerCount * 8, 64);
        long sampler_originZ_offset = MemoryUtil.roundUp(sampler_originY_offset + nonNullSamplerCount * 8, 64);
        long amplitudes_offset = MemoryUtil.roundUp(sampler_originZ_offset + nonNullSamplerCount * 8, 64);
        ByteBuffer data = alloc(MemoryUtil.roundUp(amplitudes_offset + nonNullSamplerCount * 8, 64), 64);
        data.order(ByteOrder.nativeOrder());
        putLong(data, double_octave_sampler_data$length, nonNullSamplerCount);
        putDouble(data, double_octave_sampler_data$amplitude, amplitude);
        putInt(data, double_octave_sampler_data$need_shift, (int) need_shift_offset);
        putInt(data, double_octave_sampler_data$lacunarity_powd, (int) lacunarity_powd_offset);
        putInt(data, double_octave_sampler_data$persistence_powd, (int) persistence_powd_offset);
        putInt(data, double_octave_sampler_data$sampler_permutations, (int) sampler_permutations_offset);
        putInt(data, double_octave_sampler_data$sampler_originX, (int) sampler_originX_offset);
        putInt(data, double_octave_sampler_data$sampler_originY, (int) sampler_originY_offset);
        putInt(data, double_octave_sampler_data$sampler_originZ, (int) sampler_originZ_offset);
        putInt(data, double_octave_sampler_data$amplitudes, (int) amplitudes_offset);

        long index = 0;
        {
            ImprovedNoise[] octaveSamplers = ((IOctavePerlinNoiseSampler) firstSampler).getOctaveSamplers();
            for (int i = 0; i < octaveSamplers.length; i++) {
                ImprovedNoise sampler = octaveSamplers[i];
                if (sampler != null) {
                    data.put((int) (need_shift_offset + index), (byte) 0);
                    putDouble(data, (int) lacunarity_powd_offset + (int) index * 8,
                        ((IOctavePerlinNoiseSampler) firstSampler).getLacunarity() * Math.pow(2.0, i));
                    putDouble(data, (int) persistence_powd_offset + (int) index * 8,
                        ((IOctavePerlinNoiseSampler) firstSampler).getPersistence() * Math.pow(2.0, -i));
                    byte[] perm = ((IPerlinNoiseSampler) (Object) sampler).getPermutation();
                    for (int j = 0; j < 256; j++) {
                        data.put((int) (sampler_permutations_offset + index * 256 + j), perm[j]);
                    }
                    putDouble(data, (int) sampler_originX_offset + (int) index * 8, sampler.xo);
                    putDouble(data, (int) sampler_originY_offset + (int) index * 8, sampler.yo);
                    putDouble(data, (int) sampler_originZ_offset + (int) index * 8, sampler.zo);
                    putDouble(data, (int) amplitudes_offset + (int) index * 8,
                        ((IOctavePerlinNoiseSampler) firstSampler).getAmplitudes().getDouble(i));
                    index++;
                }
            }
        }
        {
            ImprovedNoise[] octaveSamplers = ((IOctavePerlinNoiseSampler) secondSampler).getOctaveSamplers();
            for (int i = 0; i < octaveSamplers.length; i++) {
                ImprovedNoise sampler = octaveSamplers[i];
                if (sampler != null) {
                    data.put((int) (need_shift_offset + index), (byte) 1);
                    putDouble(data, (int) lacunarity_powd_offset + (int) index * 8,
                        ((IOctavePerlinNoiseSampler) secondSampler).getLacunarity() * Math.pow(2.0, i));
                    putDouble(data, (int) persistence_powd_offset + (int) index * 8,
                        ((IOctavePerlinNoiseSampler) secondSampler).getPersistence() * Math.pow(2.0, -i));
                    byte[] perm = ((IPerlinNoiseSampler) (Object) sampler).getPermutation();
                    for (int j = 0; j < 256; j++) {
                        data.put((int) (sampler_permutations_offset + index * 256 + j), perm[j]);
                    }
                    putDouble(data, (int) sampler_originX_offset + (int) index * 8, sampler.xo);
                    putDouble(data, (int) sampler_originY_offset + (int) index * 8, sampler.yo);
                    putDouble(data, (int) sampler_originZ_offset + (int) index * 8, sampler.zo);
                    putDouble(data, (int) amplitudes_offset + (int) index * 8,
                        ((IOctavePerlinNoiseSampler) secondSampler).getAmplitudes().getDouble(i));
                    index++;
                }
            }
        }
        return data;
    }

    public static ByteBuffer interpolated_noise_sampler$create(BlendedNoise interpolated, boolean unused) {
        IInterpolatedNoiseSampler acc = (IInterpolatedNoiseSampler) interpolated;
        // pre-compute total size
        int total = interpolated_noise_sampler_SIZE;
        total = subSamplerSize(total, acc.getLowerInterpolatedNoise());
        total = subSamplerSize(total, acc.getUpperInterpolatedNoise());
        total = subSamplerSize(total, acc.getInterpolationNoise());
        ByteBuffer data = alloc(total, 64);
        data.order(ByteOrder.nativeOrder());
        putDouble(data, interpolated_noise_sampler$scaledXzScale, acc.getScaledXzScale());
        putDouble(data, interpolated_noise_sampler$scaledYScale, acc.getScaledYScale());
        putDouble(data, interpolated_noise_sampler$xzFactor, acc.getXzFactor());
        putDouble(data, interpolated_noise_sampler$yFactor, acc.getYFactor());
        putDouble(data, interpolated_noise_sampler$smearScaleMultiplier, acc.getSmearScaleMultiplier());
        putDouble(data, interpolated_noise_sampler$xzScale, acc.getXzScale());
        putDouble(data, interpolated_noise_sampler$yScale, acc.getYScale());
        int currentOffset = interpolated_noise_sampler_SIZE;
        currentOffset = writeSubSampler(data, interpolated_noise_sampler$lower, currentOffset, acc.getLowerInterpolatedNoise(), 0);
        currentOffset = writeSubSampler(data, interpolated_noise_sampler$upper, currentOffset, acc.getUpperInterpolatedNoise(), 8);
        currentOffset = writeSubSampler(data, interpolated_noise_sampler$normal, currentOffset, acc.getInterpolationNoise(), 16);
        return data;
    }

    private static int subSamplerSize(int currentOffset, PerlinNoise octaveNoise) {
        ImprovedNoise[] octaveSamplers = ((IOctavePerlinNoiseSampler) octaveNoise).getOctaveSamplers();
        int count = 0;
        for (ImprovedNoise s : octaveSamplers) if (s != null) count++;
        int permOffset = MemoryUtil.roundUp(currentOffset, 64);
        int originXOffset = MemoryUtil.roundUp(permOffset + count * 256, 64);
        int originYOffset = MemoryUtil.roundUp(originXOffset + count * 8, 64);
        int originZOffset = MemoryUtil.roundUp(originYOffset + count * 8, 64);
        int mulFactorOffset = MemoryUtil.roundUp(originZOffset + count * 8, 64);
        return mulFactorOffset + count * 8;
    }

    private static int writeSubSampler(ByteBuffer data, int structOffset, int currentOffset, PerlinNoise octaveNoise, int baseOctave) {
        ImprovedNoise[] octaveSamplers = ((IOctavePerlinNoiseSampler) octaveNoise).getOctaveSamplers();
        int count = 0;
        for (ImprovedNoise s : octaveSamplers) if (s != null) count++;
        int permOffset = MemoryUtil.roundUp(currentOffset, 64);
        int originXOffset = MemoryUtil.roundUp(permOffset + count * 256, 64);
        int originYOffset = MemoryUtil.roundUp(originXOffset + count * 8, 64);
        int originZOffset = MemoryUtil.roundUp(originYOffset + count * 8, 64);
        int mulFactorOffset = MemoryUtil.roundUp(originZOffset + count * 8, 64);
        putInt(data, structOffset + interpolated_noise_sub_sampler$length, count);
        putInt(data, structOffset + interpolated_noise_sub_sampler$sampler_permutations, permOffset);
        putInt(data, structOffset + interpolated_noise_sub_sampler$sampler_originX, originXOffset);
        putInt(data, structOffset + interpolated_noise_sub_sampler$sampler_originY, originYOffset);
        putInt(data, structOffset + interpolated_noise_sub_sampler$sampler_originZ, originZOffset);
        putInt(data, structOffset + interpolated_noise_sub_sampler$sampler_mulFactor, mulFactorOffset);
        int idx = 0;
        for (int i = 0; i < octaveSamplers.length; i++) {
            ImprovedNoise sampler = octaveSamplers[i];
            if (sampler == null) continue;
            byte[] perm = ((IPerlinNoiseSampler) (Object) sampler).getPermutation();
            for (int j = 0; j < 256; j++) data.put(permOffset + idx * 256 + j, perm[j]);
            putDouble(data, originXOffset + idx * 8, sampler.xo);
            putDouble(data, originYOffset + idx * 8, sampler.yo);
            putDouble(data, originZOffset + idx * 8, sampler.zo);
            putDouble(data, mulFactorOffset + idx * 8, Math.pow(2, -(baseOctave + i)));
            idx++;
        }
        return mulFactorOffset + count * 8;
    }

    public static final class NativeBiomeSearchTree {
        public final ByteBuffer segment;
        public final int node_c;
        public final int tree_depth;
        public final Holder<Biome>[] biomes;

        public NativeBiomeSearchTree(ByteBuffer segment, int node_c, int tree_depth, Holder<Biome>[] biomes) {
            this.segment = segment;
            this.node_c = node_c;
            this.tree_depth = tree_depth;
            this.biomes = biomes;
        }
    }

    public static NativeBiomeSearchTree biome_search_tree_node$create(RTree<Holder<Biome>> searchTree) {
        class SerializedTreeNode {
            boolean isBranch;
            boolean isChildrenOffsets = false;
            int biomeId;
            final short[] maxs = new short[7];
            final short[] mins = new short[7];
            final int[] childrenOffsets = new int[7];
        }
        class TreeFlattener {
            final List<SerializedTreeNode> nodes = new ArrayList<>();
            final Object2IntLinkedOpenHashMap<Holder<Biome>> biomeIdMap = new Object2IntLinkedOpenHashMap<>();
            int treeDepth;

            TreeFlattener() {
                this.biomeIdMap.defaultReturnValue(Integer.MIN_VALUE);
                this.nodes.add(new SerializedTreeNode());
            }

            @SuppressWarnings("unchecked")
            int consume(Object node, int depth) {
                Objects.requireNonNull(node, "node cannot be null");
                if (depth > this.treeDepth) this.treeDepth = depth;
                SerializedTreeNode serializedNode = new SerializedTreeNode();
                Parameter[] parameters = ((com.ishland.c2me.opts.accel.opencl.mixin.access.IRTreeNodeAccess) node).c2me$getParameterSpace();
                if (parameters.length != 7) throw new IllegalStateException("Expected 7 parameters, got " + parameters.length);
                for (int i = 0; i < 7; i++) {
                    serializedNode.maxs[i] = (short) (int) parameters[i].max();
                    serializedNode.mins[i] = (short) (int) parameters[i].min();
                }
                int index = this.nodes.size();
                this.nodes.add(serializedNode);
                Object[] children = node instanceof net.minecraft.world.level.biome.Climate.RTree.SubTree ? ((com.ishland.c2me.opts.accel.opencl.mixin.access.IRTreeSubTreeAccess) node).c2me$getChildren() : null;
                if (children != null) {
                    serializedNode.isBranch = true;
                    SerializedTreeNode childrenOffsetNode = new SerializedTreeNode();
                    childrenOffsetNode.isBranch = true;
                    childrenOffsetNode.isChildrenOffsets = true;
                    Arrays.fill(childrenOffsetNode.childrenOffsets, 0);
                    this.nodes.add(childrenOffsetNode);
                    if (children.length > 7) throw new IllegalStateException("Too many children");
                    for (int i = 0; i < children.length; i++) {
                        Object child = children[i];
                        if (child != null) childrenOffsetNode.childrenOffsets[i] = this.consume(child, depth + 1);
                    }
                } else {
                    Object value = ((com.ishland.c2me.opts.accel.opencl.mixin.access.IRTreeLeafAccess) node).c2me$getValue();
                    Holder<Biome> biome = (Holder<Biome>) value;
                    int biomeId = this.biomeIdMap.computeIfAbsent(biome, var1 -> this.biomeIdMap.size());
                    serializedNode.isBranch = false;
                    serializedNode.biomeId = biomeId;
                }
                return index;
            }

            void validate() {
                Iterator<SerializedTreeNode> iterator = this.nodes.iterator();
                iterator.next();
                while (iterator.hasNext()) {
                    SerializedTreeNode node = iterator.next();
                    if (node.isBranch) {
                        if (!iterator.hasNext()) throw new IllegalStateException("Branch node must have children offsets in the next node");
                        SerializedTreeNode childrenOffsetsNode = iterator.next();
                        if (!childrenOffsetsNode.isBranch || !childrenOffsetsNode.isChildrenOffsets) {
                            throw new IllegalStateException("Branch node must have children offsets in the next node");
                        }
                    }
                }
            }
        }

        TreeFlattener treeFlattener = new TreeFlattener();
        treeFlattener.consume(((com.ishland.c2me.opts.accel.opencl.mixin.access.IRTreeAccess) (Object) searchTree).c2me$getRoot(), 1);
        treeFlattener.validate();
        @SuppressWarnings("unchecked")
        Holder<Biome>[] biomes = new Holder[treeFlattener.biomeIdMap.size()];
        ObjectBidirectionalIterator<Entry<Holder<Biome>>> iterator = treeFlattener.biomeIdMap.object2IntEntrySet().fastIterator();
        while (iterator.hasNext()) {
            Entry<Holder<Biome>> entry = iterator.next();
            if (biomes[entry.getIntValue()] != null) throw new IllegalStateException("Duplicate biome ID found");
            biomes[entry.getIntValue()] = entry.getKey();
        }

        ByteBuffer segment = alloc((long) treeFlattener.nodes.size() * biome_search_tree_node_SIZE, 64);
        segment.order(ByteOrder.nativeOrder());
        List<SerializedTreeNode> nodes = treeFlattener.nodes;
        for (int i = 0; i < nodes.size(); i++) {
            SerializedTreeNode node = nodes.get(i);
            int state = (node.isBranch ? Integer.MIN_VALUE : 0) | (node.isChildrenOffsets ? 0x40000000 : 0) | node.biomeId;
            putInt(segment, i * biome_search_tree_node_SIZE + biome_search_tree_node$state, state);
            if (node.isChildrenOffsets) {
                for (int j = 0; j < 7; j++) {
                    putInt(segment, i * biome_search_tree_node_SIZE + biome_search_tree_node$children_offset + j * 4, node.childrenOffsets[j]);
                }
            } else {
                for (int j = 0; j < 7; j++) {
                    segment.putShort(i * biome_search_tree_node_SIZE + biome_search_tree_node$maxs + j * 2, node.maxs[j]);
                    segment.putShort(i * biome_search_tree_node_SIZE + biome_search_tree_node$mins + j * 2, node.mins[j]);
                }
            }
        }
        return new NativeBiomeSearchTree(segment, nodes.size(), treeFlattener.treeDepth, biomes);
    }
}
