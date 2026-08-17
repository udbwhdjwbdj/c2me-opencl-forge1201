package com.ishland.c2me.opts.accel.opencl.common.compiler;

import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2IntLinkedOpenHashMap;
import java.nio.file.Path;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

public class GeneratedCLSource {
   private final long ordinal;
   private final String generatedSource;
   private final byte[] constData;
   private final Reference2IntLinkedOpenHashMap<Object> globalDynamicDataOffsets;
   private final int flatCachePrefills;
   private final int cache2dPrefills;
   private final int interpolatorPrefills;
   private final Object2ReferenceOpenHashMap<String, String> defines;
   private final Holder<Biome>[] biomeMappings;
   private final Path dumpedPath;

   public GeneratedCLSource(
      long ordinal,
      String generatedSource,
      byte[] constData,
      Reference2IntLinkedOpenHashMap<Object> globalDynamicDataOffsets,
      int flatCachePrefills,
      int cache2dPrefills,
      int interpolatorPrefills,
      Object2ReferenceOpenHashMap<String, String> defines,
      Holder<Biome>[] biomeMappings,
      Path dumpedPath
   ) {
      this.ordinal = ordinal;
      this.generatedSource = generatedSource;
      this.constData = constData;
      this.globalDynamicDataOffsets = globalDynamicDataOffsets;
      this.flatCachePrefills = flatCachePrefills;
      this.cache2dPrefills = cache2dPrefills;
      this.interpolatorPrefills = interpolatorPrefills;
      this.defines = defines;
      this.biomeMappings = biomeMappings;
      this.dumpedPath = dumpedPath;
   }

   public long getOrdinal() {
      return this.ordinal;
   }

   public String getGeneratedSource() {
      return this.generatedSource;
   }

   public byte[] getConstData() {
      return this.constData;
   }

   public Reference2IntLinkedOpenHashMap<Object> getGlobalDynamicDataOffsets() {
      return this.globalDynamicDataOffsets;
   }

   public int getFlatCachePrefills() {
      return this.flatCachePrefills;
   }

   public int getInterpolatorPrefills() {
      return this.interpolatorPrefills;
   }

   public int getCache2dPrefills() {
      return this.cache2dPrefills;
   }

   public Object2ReferenceOpenHashMap<String, String> getDefines() {
      return this.defines;
   }

   public Holder<Biome>[] getBiomeMappings() {
      return this.biomeMappings;
   }

   public Path getDumpedPath() {
      return this.dumpedPath;
   }
}
