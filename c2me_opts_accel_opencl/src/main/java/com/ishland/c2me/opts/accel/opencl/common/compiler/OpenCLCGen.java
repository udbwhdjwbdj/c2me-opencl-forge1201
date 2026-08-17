package com.ishland.c2me.opts.accel.opencl.common.compiler;

import com.ishland.c2me.opts.accel.opencl.common.util.MemoryUtil;
import com.ishland.c2me.opts.accel.opencl.mixin.access.IDoublePerlinNoiseSampler;
import com.ishland.c2me.opts.accel.opencl.mixin.access.IMultiNoiseUtilEntries;
import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.McToAst;
import com.ishland.c2me.opts.dfc.common.ast.AstNode.ReturnType;
import com.ishland.c2me.opts.dfc.common.ast.misc.CacheLikeNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.ConstantNodeLike;
import com.ishland.c2me.opts.dfc.common.ast.opto.OptoPasses;
import com.ishland.c2me.opts.dfc.common.ast.opto.OptoPasses.AstPair;
import com.ishland.c2me.opts.dfc.common.gen.GenDumper;
import com.ishland.c2me.opts.dfc.common.gen.backports.FindTopSurface;
import com.ishland.c2me.opts.dfc.common.gen.meta.ValuesMethodDef;
import com.ishland.c2me.opts.dfc.common.gen.meta.ValuesMethodDefF32;
import com.ishland.c2me.opts.dfc.common.gen.meta.ValuesMethodDefF64;
import com.ishland.c2me.opts.dfc.common.gen.opencl.OpenCLCGenContext;
import com.ishland.c2me.opts.dfc.common.gen.opencl.OpenCLCGenFunctionContext;
import com.ishland.c2me.opts.dfc.common.gen.opencl.OpenCLCGenFunctionContext.FunctionVariant;
import com.ishland.c2me.opts.natives_math.common.BindingsTemplate;
import com.ishland.c2me.opts.natives_math.common.BindingsTemplate.NativeBiomeSearchTree;
import com.ishland.c2me.opts.accel.opencl.common.util.FlowschedAssertions;
import it.unimi.dsi.fastutil.Hash.Strategy;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.Climate.ParameterList;
import net.minecraft.world.level.biome.Climate.RTree;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.DensityFunctions.BeardifierMarker;
import net.minecraft.world.level.levelgen.DensityFunctions.MarkerOrMarked;
import net.minecraft.world.level.levelgen.DensityFunctions.Marker.Type;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public class OpenCLCGen {
   public static final Object MARKER_localOffsetTable = new Object();
   public static final Object MARKER_estimateSurfaceHeightCache = new Object();
   public static final Object MARKER_aquifer = new Object();
   public static final Object MARKER_fluidLevelSampler = new Object();
   public static final Object MARKER_oreVeinRandom = new Object();
   public static final Object MARKER_cacheLike_interpolator = new Object();
   public static final Object MARKER_cacheLike_flatCache = new Object();
   public static final Object MARKER_cacheLike_cache2d = new Object();
   private static final AtomicLong ordinal = new AtomicLong();

   public static GeneratedCLSource compile(
      NoiseRouter noiseRouter,
      NoiseSettings generationShapeConfig,
      Reference2ReferenceMap<DensityFunction, AstPair> optoCache,
      DensityFunction finalFinalDensity,
      BiomeSource biomeSource
   ) {
      OpenCLCGen.ContextImpl context = new OpenCLCGen.ContextImpl();
      context.prependConstants(generationShapeConfig);
      Object2ReferenceLinkedOpenHashMap<String, AstPair> dfs = new Object2ReferenceLinkedOpenHashMap();
      dfs.put("barrier", optimizeCached(noiseRouter.barrierNoise(), optoCache));
      dfs.put("fluid_level_floodedness", optimizeCached(noiseRouter.fluidLevelFloodednessNoise(), optoCache));
      dfs.put("fluid_level_spread", optimizeCached(noiseRouter.fluidLevelSpreadNoise(), optoCache));
      dfs.put("lava", optimizeCached(noiseRouter.lavaNoise(), optoCache));
      dfs.put("temperature", optimizeCached(noiseRouter.temperature(), optoCache));
      dfs.put("vegetation", optimizeCached(noiseRouter.vegetation(), optoCache));
      dfs.put("continents", optimizeCached(noiseRouter.continents(), optoCache));
      dfs.put("erosion", optimizeCached(noiseRouter.erosion(), optoCache));
      dfs.put("depth", optimizeCached(noiseRouter.depth(), optoCache));
      dfs.put("ridges", optimizeCached(noiseRouter.ridges(), optoCache));
      dfs.put(
         "preliminary_surface_level",
         optimizeCached(adaptPreliminarySurfaceLevel(generationShapeConfig, noiseRouter.initialDensityWithoutJaggedness()), optoCache)
      );
      dfs.put("final_density", optimizeCached(noiseRouter.finalDensity(), optoCache));
      dfs.put("vein_toggle", optimizeCached(noiseRouter.veinToggle(), optoCache));
      dfs.put("vein_ridged", optimizeCached(noiseRouter.veinRidged(), optoCache));
      dfs.put("vein_gap", optimizeCached(noiseRouter.veinGap(), optoCache));
      dfs.put("final_final_density", optimizeCached(finalFinalDensity, optoCache));
      ObjectBidirectionalIterator original = dfs.entrySet().iterator();

      while (original.hasNext()) {
         Entry<String, AstPair> entry = (Entry<String, AstPair>)original.next();
         context.compileBinding(entry.getValue().optimized(), entry.getKey());
      }

      context.genNoiseKernels();
      context.genBiomeTree(biomeSource);
      GeneratedCLSource originalx = context.build();
      String name = "DfcCompiled_" + originalx.getOrdinal();
      Path path = GenDumper.dumpCL(name, originalx.getGeneratedSource().getBytes(StandardCharsets.UTF_8));
      GenDumper.dumpDot(name, path, dfs, o -> {
         StringBuilder builder = (StringBuilder)context.recordedAuxNames.get(o);
         return builder != null && !builder.isEmpty() ? builder.toString() : null;
      });
      return new GeneratedCLSource(
         originalx.getOrdinal(),
         originalx.getGeneratedSource(),
         originalx.getConstData(),
         originalx.getGlobalDynamicDataOffsets(),
         originalx.getFlatCachePrefills(),
         originalx.getCache2dPrefills(),
         originalx.getInterpolatorPrefills(),
         originalx.getDefines(),
         originalx.getBiomeMappings(),
         path
      );
   }

   private static DensityFunction adaptPreliminarySurfaceLevel(NoiseSettings config, DensityFunction initialDensityWithoutJaggedness) {
      return new FindTopSurface(
         DensityFunctions.add(DensityFunctions.constant(-0.390625), initialDensityWithoutJaggedness),
         DensityFunctions.constant((double)(config.height() - config.minY())),
         config.minY(),
         config.noiseSizeHorizontal()
      );
   }

   private static AstPair optimizeCached(DensityFunction densityFunction, Reference2ReferenceMap<DensityFunction, AstPair> optoCache) {
      return (AstPair)optoCache.computeIfAbsent(densityFunction, df -> OptoPasses.optimizeOCL(McToAst.toAst((net.minecraft.world.level.levelgen.DensityFunction) df)));
   }

   private static void validateNodeType(AstNode node, ReturnType returnType) {
      if (node.getReturnType() != returnType) {
         throw new IllegalArgumentException("Invalid descriptor: tried to store %s into %s".formatted(node.getReturnType(), returnType));
      }
   }

   private static void validateTarget(ValuesMethodDef target, ReturnType returnType) {
      if (target.returnType() != returnType) {
         throw new IllegalArgumentException("Invalid descriptor: tried to store %s into %s".formatted(target.returnType(), returnType));
      }
   }

   private static ValuesMethodDef makeValuesMethodDef(String name, ReturnType returnType) {
      return (ValuesMethodDef)(switch (returnType) {
         case F64 -> new ValuesMethodDefF64(name);
         case F32 -> new ValuesMethodDefF32(name);
         default -> throw new IllegalStateException("Unexpected type");
      });
   }

   private static String getDataType(ReturnType returnType) {
      return switch (returnType) {
         case F64 -> "double";
         case F32 -> "float";
         default -> throw new IllegalStateException("Unexpected type");
      };
   }

   public static String literal(double d) {
      return Double.toHexString(d);
   }

   public static String literal(float d) {
      return Float.toHexString(d) + "f";
   }

   public static String literal(byte[] bytes) {
      StringBuilder builder = new StringBuilder();
      builder.append("{ ");
      HexFormat.of().withPrefix("0x").withDelimiter(", ").formatHex(builder, bytes);
      builder.append(" }");
      return builder.toString();
   }

   public static String literal(int[] ints) {
      StringBuilder builder = new StringBuilder();
      builder.append("{ ");

      for (int num : ints) {
         builder.append(num).append(", ");
      }

      builder.append(" }");
      return builder.toString();
   }

   public static String literal(float[] floats) {
      StringBuilder builder = new StringBuilder();
      builder.append("{ ");

      for (float f : floats) {
         builder.append(Float.toHexString(f)).append("f, ");
      }

      builder.append("}");
      return builder.toString();
   }

   private static Object reflectBiomeEntries(Object source) {
      return ((com.ishland.c2me.opts.accel.opencl.mixin.access.IMultiNoiseBiomeSourceAccess) source).c2me$parameters();
   }

   public static byte[] bytes(NormalNoise sampler) {
      ByteBuffer memorySegment = BindingsTemplate.double_octave_sampler_data$create(
         ((IDoublePerlinNoiseSampler) sampler).getFirstSampler(),
         ((IDoublePerlinNoiseSampler) sampler).getSecondSampler(),
         ((IDoublePerlinNoiseSampler) sampler).getAmplitude(),
         true
      );
      byte[] bytes = new byte[memorySegment.capacity()];
      memorySegment.get(0, bytes, 0, bytes.length);
      return bytes;
   }

   public static byte[] bytes(BlendedNoise sampler) {
      ByteBuffer memorySegment = BindingsTemplate.interpolated_noise_sampler$create(sampler, true);
      byte[] bytes = new byte[memorySegment.capacity()];
      memorySegment.get(0, bytes, 0, bytes.length);
      return bytes;
   }

   public static byte[] bytes(int[] ints) {
      ByteBuffer bb = ByteBuffer.allocate(ints.length * 4).order(ByteOrder.nativeOrder());
      for (int i : ints) bb.putInt(i);
      return bb.array();
   }

   public static byte[] bytes(float[] floats) {
      ByteBuffer bb = ByteBuffer.allocate(floats.length * 4).order(ByteOrder.nativeOrder());
      for (float f : floats) bb.putFloat(f);
      return bb.array();
   }

   public static byte[] bytesObject(Object object) {
      Objects.requireNonNull(object);

      if (object instanceof BlendedNoise sampler) {
         return bytes(sampler);
      }
      if (object instanceof NormalNoise samplerx) {
         return bytes(samplerx);
      }
      if (object instanceof int[] ints) {
         return bytes(ints);
      }
      if (object instanceof float[] floats) {
         return bytes(floats);
      }
      throw new UnsupportedOperationException(object.getClass().getName());
   }

   public static class ContextImpl implements OpenCLCGenContext {
      private final StringBuilder pendingSource = new StringBuilder();
      private final Object2ReferenceOpenHashMap<OpenCLCGen.ContextImpl.FunctionKey, String> methods = new Object2ReferenceOpenHashMap();
      private final Reference2IntLinkedOpenHashMap<Object> globalDynamicDataOffsets = new Reference2IntLinkedOpenHashMap();
      private final Object2IntLinkedOpenHashMap<CacheLikeNode> flatCaches = new Object2IntLinkedOpenHashMap();
      private final Object2IntLinkedOpenHashMap<CacheLikeNode> interpolators = new Object2IntLinkedOpenHashMap();
      private final Object2IntLinkedOpenHashMap<CacheLikeNode> cache2ds = new Object2IntLinkedOpenHashMap();
      private final Object2ReferenceOpenHashMap<String, String> defines = new Object2ReferenceOpenHashMap();
      private final Object2ReferenceOpenHashMap<Object, StringBuilder> recordedAuxNames = new Object2ReferenceOpenHashMap();
      private Holder<Biome>[] biomeMappings = null;
      private boolean cacheFrozen = false;
      private final int localOffsetTableOffset = this.allocGlobalDynamicData(OpenCLCGen.MARKER_localOffsetTable);
      private int globalConstDataTail = 16;
      private final Object2IntOpenCustomHashMap<byte[]> globalConstDataOffsets = new Object2IntOpenCustomHashMap(new Strategy<byte[]>() {
         {
            Objects.requireNonNull(ContextImpl.this);
         }

         public int hashCode(byte[] o) {
            return Arrays.hashCode(o);
         }

         public boolean equals(byte[] a, byte[] b) {
            return Arrays.equals(a, b);
         }
      });
      private int methodIdx;

      public ContextImpl() {
         this.globalDynamicDataOffsets.defaultReturnValue(Integer.MAX_VALUE);
         this.allocGlobalDynamicData(BeardifierMarker.INSTANCE);
         this.allocGlobalDynamicData(OpenCLCGen.MARKER_estimateSurfaceHeightCache);
         this.allocGlobalDynamicData(OpenCLCGen.MARKER_aquifer);
         this.allocGlobalDynamicData(OpenCLCGen.MARKER_fluidLevelSampler);
         this.allocGlobalDynamicData(OpenCLCGen.MARKER_oreVeinRandom);
         this.allocGlobalDynamicData(OpenCLCGen.MARKER_cacheLike_flatCache);
         this.allocGlobalDynamicData(OpenCLCGen.MARKER_cacheLike_cache2d);
         this.allocGlobalDynamicData(OpenCLCGen.MARKER_cacheLike_interpolator);
         this.flatCaches.defaultReturnValue(-1);
         this.interpolators.defaultReturnValue(-1);
         this.cache2ds.defaultReturnValue(-1);
         this.methodIdx = 0;
      }

      public String nextMethodName() {
         return String.format("method_%d", this.methodIdx++);
      }

      public String nextMethodName(String suffix) {
         return String.format("method_%d_%s", this.methodIdx++, suffix);
      }

      public void prependConstants(NoiseSettings generationShapeConfig) {
         this.pendingSource
            .append("constant const int32_t genShapeCfg_minimumY = ")
            .append(generationShapeConfig.minY())
            .append(";\n")
            .append("constant const int32_t genShapeCfg_height = ")
            .append(generationShapeConfig.height())
            .append(";\n")
            .append("constant const uint32_t genShapeCfg_horizontalSize = ")
            .append(generationShapeConfig.noiseSizeHorizontal())
            .append(";\n")
            .append("constant const uint32_t genShapeCfg_verticalSize = ")
            .append(generationShapeConfig.noiseSizeVertical())
            .append(";\n");
      }

      public void compileBinding(AstNode node, String id) {
         this.newDispatcherF64(node, "df_binding_" + id);
      }

      public ValuesMethodDef newDispatcher(AstNode node, String id, ReturnType returnType) {
         OpenCLCGen.validateNodeType(node, returnType);

         for (FunctionVariant variant : FunctionVariant.values()) {
            if (variant.inDispatcher) {
               ValuesMethodDef method = this.newMethod(node, variant, node.getReturnType());
               this.pendingSource
                  .append("static __attribute__((pure)) ")
                  .append(OpenCLCGen.getDataType(node.getReturnType()))
                  .append(" ")
                  .append(id)
                  .append(variant.suffix)
                  .append("(const sample_int32_ctx_t ctx)")
                  .append(" {\n")
                  .append("    ")
                  .append("return ")
                  .append(this.callDelegate(method, node.getReturnType()))
                  .append(";\n")
                  .append("}\n");
            }
         }

         this.pendingSource
            .append("static __attribute__((pure)) ")
            .append(OpenCLCGen.getDataType(node.getReturnType()))
            .append(" ")
            .append(id)
            .append("(const sample_int32_ctx_t ctx)")
            .append(" {\n")
            .append("    ")
            .append("if (ctx.rw_data && (ctx.sample_flags & MASK_enableAllCaches) == MASK_enableAllCaches) {\n")
            .append("    ")
            .append("    ")
            .append("return ")
            .append(id)
            .append(FunctionVariant.FULLY_CACHED.suffix)
            .append("(ctx);\n")
            .append("    ")
            .append("} else if (ctx.rw_data &&(ctx.sample_flags & MASK_enableFlatCache) == MASK_enableFlatCache) {\n")
            .append("    ")
            .append("    ")
            .append("return ")
            .append(id)
            .append(FunctionVariant.FLATCACHE_ONLY.suffix)
            .append("(ctx);\n")
            .append("    ")
            .append("} else {\n")
            .append("    ")
            .append("    ")
            .append("return ")
            .append(id)
            .append(FunctionVariant.UNCACHED.suffix)
            .append("(ctx);\n")
            .append("    ")
            .append("}\n")
            .append("}\n");
         return OpenCLCGen.makeValuesMethodDef(id, node.getReturnType());
      }

      public ValuesMethodDefF64 newDispatcherF64(AstNode node) {
         return this.newDispatcherF64(node, this.nextMethodName());
      }

      public ValuesMethodDefF64 newDispatcherF64(AstNode node, String id) {
         return (ValuesMethodDefF64)this.newDispatcher(node, id, ReturnType.F64);
      }

      public ValuesMethodDefF32 newDispatcherF32(AstNode node) {
         return this.newDispatcherF32(node, this.nextMethodName());
      }

      public ValuesMethodDefF32 newDispatcherF32(AstNode node, String id) {
         return (ValuesMethodDefF32)this.newDispatcher(node, id, ReturnType.F32);
      }

      public ValuesMethodDef newMethod(AstNode node, FunctionVariant variant, ReturnType returnType) {
         OpenCLCGen.validateNodeType(node, returnType);
         if (node instanceof ConstantNodeLike constantNodeLike) {
            return constantNodeLike.getDef();
         } else {
            String generated = this.newMethodUnoptimized(node, variant);
            return OpenCLCGen.makeValuesMethodDef(generated, returnType);
         }
      }

      public ValuesMethodDefF64 newMethodF64(AstNode node, FunctionVariant variant) {
         return (ValuesMethodDefF64)this.newMethod(node, variant, ReturnType.F64);
      }

      public ValuesMethodDefF32 newMethodF32(AstNode node, FunctionVariant variant) {
         return (ValuesMethodDefF32)this.newMethod(node, variant, ReturnType.F32);
      }

      public String newMethodUnoptimized(AstNode node, FunctionVariant variant) {
         return (String)this.methods.computeIfAbsent(new OpenCLCGen.ContextImpl.FunctionKey(node, variant), this::newMethod0);
      }

      private String newMethod0(OpenCLCGen.ContextImpl.FunctionKey key) {
         String methodName = this.nextMethodName();
         OpenCLCGen.FunctionContextImpl functionContext = new OpenCLCGen.FunctionContextImpl(this, null, methodName, key.variant());
         ValuesMethodDef finalVar = functionContext.newVar(key.node());
         this.pendingSource
            .append("static __attribute__((pure)) ")
            .append(OpenCLCGen.getDataType(key.node().getReturnType()))
            .append(" ")
            .append(methodName)
            .append("(const sample_int32_ctx_t ctx)")
            .append(" {\n")
            .append(functionContext.pendingBody.toString().indent(4))
            .append("    ")
            .append("return ")
            .append(functionContext.getDelegateVar(finalVar, key.node().getReturnType()))
            .append(";\n")
            .append("}\n");
         return methodName;
      }

      public String callDelegate(ValuesMethodDef target, ReturnType returnType) {
         OpenCLCGen.validateTarget(target, returnType);
         if (target.isConst()) {
            Objects.requireNonNull(target);

            if (target instanceof ValuesMethodDefF32 f32) {
               return OpenCLCGen.literal(f32.constValue());
            }
            if (target instanceof ValuesMethodDefF64 f64) {
               return OpenCLCGen.literal(f64.constValue());
            }
            throw new IllegalStateException("Unexpected type: " + target.getClass().getName());
         } else {
            return target.generatedMethod() + "(ctx)";
         }
      }

      public String callDelegate(ValuesMethodDefF64 target) {
         return this.callDelegate(target, ReturnType.F64);
      }

      public String callDelegate(ValuesMethodDefF32 target) {
         return this.callDelegate(target, ReturnType.F32);
      }

      public int allocGlobalDynamicData(Object data) {
         if (this.globalDynamicDataOffsets.containsKey(data)) {
            return this.globalDynamicDataOffsets.getInt(data);
         } else {
            int ordinal = this.globalDynamicDataOffsets.size();
            this.globalDynamicDataOffsets.put(data, ordinal);
            return ordinal;
         }
      }

      public int allocGlobalConstData(byte[] data, int alignment) {
         if (this.globalConstDataOffsets.containsKey(data)) {
            return this.globalConstDataOffsets.getInt(data);
         } else {
            int startOffset = this.globalConstDataTail;
            this.globalConstDataTail = MemoryUtil.roundUp(this.globalConstDataTail, alignment) + data.length;
            this.globalConstDataOffsets.put(data, startOffset);
            return startOffset;
         }
      }

      public int allocGlobalConstDataObject(Object obj) {
         byte[] bytes = OpenCLCGen.bytesObject(obj);
         return this.allocGlobalConstData(bytes, 8);
      }

      public int getGlobalDynamicDataOffset(Object data) {
         if (!this.globalDynamicDataOffsets.containsKey(data)) {
            throw new IllegalStateException("No global dynamic data offset found");
         } else {
            return this.globalDynamicDataOffsets.getInt(data);
         }
      }

      private void recordAuxName(Object node, String funcName, String varName) {
         ((StringBuilder)this.recordedAuxNames.computeIfAbsent(node, unused -> new StringBuilder())).append("\\n").append(funcName).append('.').append(varName);
      }

      public int registerFlatCache(CacheLikeNode node) {
         if (node.getCacheLike() instanceof MarkerOrMarked wrapping) {
            if (wrapping.type() != Type.FlatCache) {
               throw new UnsupportedOperationException("Can only gen flat cache");
            } else {
               int index = this.flatCaches.getInt(node);
               if (index != -1) {
                  return index;
               } else if (this.cacheFrozen) {
                  throw new IllegalStateException("Cannot register more caches");
               } else {
                  index = this.flatCaches.size();
                  this.flatCaches.put(node, index);
                  return index;
               }
            }
         } else {
            throw new UnsupportedOperationException("Can only gen wrapping");
         }
      }

      public int registerCache2d(CacheLikeNode node) {
         if (node.getCacheLike() instanceof MarkerOrMarked wrapping) {
            if (wrapping.type() != Type.Cache2D) {
               throw new UnsupportedOperationException("Can only gen cache2d");
            } else {
               int index = this.cache2ds.getInt(node);
               if (index != -1) {
                  return index;
               } else if (this.cacheFrozen) {
                  throw new IllegalStateException("Cannot register more caches");
               } else {
                  index = this.cache2ds.size();
                  this.cache2ds.put(node, index);
                  return index;
               }
            }
         } else {
            throw new UnsupportedOperationException("Can only gen wrapping");
         }
      }

      public int registerInterpolator(CacheLikeNode node) {
         if (node.getCacheLike() instanceof MarkerOrMarked wrapping) {
            if (wrapping.type() != Type.Interpolated) {
               throw new UnsupportedOperationException("Can only gen interpolator");
            } else {
               int index = this.interpolators.getInt(node);
               if (index != -1) {
                  return index;
               } else if (this.cacheFrozen) {
                  throw new IllegalStateException("Cannot register more caches");
               } else {
                  index = this.interpolators.size();
                  this.interpolators.put(node, index);
                  return index;
               }
            }
         } else {
            throw new UnsupportedOperationException("Can only gen wrapping");
         }
      }

      public void genNoiseKernels() {
         this.cacheFrozen = true;
         ArrayList<String> flatCachePrefills = new ArrayList<>();
         ArrayList<String> interpolatorPrefills = new ArrayList<>();
         ArrayList<String> cache2dPrefills = new ArrayList<>();
         Object2IntLinkedOpenHashMap<CacheLikeNode> caches = this.flatCaches;
         int offset = this.getGlobalDynamicDataOffset(OpenCLCGen.MARKER_cacheLike_flatCache);
         ObjectBidirectionalIterator name = caches.object2IntEntrySet().iterator();

         while (name.hasNext()) {
            it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<CacheLikeNode> entry = (it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<CacheLikeNode>)name.next();
            CacheLikeNode node = (CacheLikeNode)entry.getKey();
            int i = entry.getIntValue();
            OpenCLCGen.validateNodeType(node, ReturnType.F64);
            String delegateName = this.newMethodUnoptimized(node.getDelegate(), FunctionVariant.UNCACHED);
            String namex = "df_flatcache_prefill_" + i;
            this.pendingSource
               .append("static void ")
               .append(namex)
               .append("(global const void * restrict const const_data, global void * restrict const rw_data, global double * restrict const extra_out) {\n")
               .append("    ")
               .append("global const worldgen_params_t * restrict params = rw_data;\n")
               .append("    ")
               .append("global double * restrict data = df_data_offset_global(rw_data, ")
               .append(offset)
               .append(");\n")
               .append("    ")
               .append("int32_t offsetX = get_global_id(0);\n")
               .append("    ")
               .append("int32_t offsetZ = get_global_id(1);\n")
               .append("    ")
               .append("uint32_t index = df_address_flatcache_buffer(params, ")
               .append(i)
               .append(", offsetX, offsetZ);\n")
               .append("    ")
               .append("const double result = ")
               .append(delegateName)
               .append(
                  "(make_sample_int32_ctx(const_data, NULL, math_biome2block(offsetX + params->startBiomeX), 0, math_biome2block(offsetZ + params->startBiomeZ), 0));\n"
               )
               .append("    ")
               .append("data[index] = result;\n")
               .append("    ")
               .append("if (extra_out) extra_out[index] = result;\n")
               .append("}\n");
            flatCachePrefills.add(namex);
         }

         caches = this.cache2ds;
         offset = this.getGlobalDynamicDataOffset(OpenCLCGen.MARKER_cacheLike_cache2d);
         name = caches.object2IntEntrySet().iterator();

         while (name.hasNext()) {
            it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<CacheLikeNode> entry = (it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<CacheLikeNode>)name.next();
            CacheLikeNode node = (CacheLikeNode)entry.getKey();
            int i = entry.getIntValue();
            OpenCLCGen.validateNodeType(node, ReturnType.F64);
            String delegateName = this.newMethodUnoptimized(node.getDelegate(), FunctionVariant.FULLY_CACHED_EXCEPT_CACHE2D);
            FlowschedAssertions.assertTrue(delegateName != null);
            String namex = "df_cache2d_prefill_" + i;
            this.pendingSource
               .append("static FUNC_NOINLINE void ")
               .append(namex)
               .append("(global const void * restrict const const_data, global void * restrict const rw_data, global double * restrict const extra_out) {\n")
               .append("    ")
               .append("global const worldgen_params_t * restrict params = rw_data;\n")
               .append("    ")
               .append("global double * restrict data = df_data_offset_global(rw_data, ")
               .append(offset)
               .append(");\n")
               .append("    ")
               .append("int32_t offsetX = get_global_id(0);\n")
               .append("    ")
               .append("int32_t offsetZ = get_global_id(1);\n")
               .append("    ")
               .append("uint32_t index = df_address_cache2d_buffer(params, ")
               .append(i)
               .append(", offsetX, offsetZ);\n")
               .append("    ")
               .append("const double result = ")
               .append(delegateName)
               .append(
                  "(make_sample_int32_ctx(const_data, rw_data, offsetX + params->cache2d_startX, 0, offsetZ + params->cache2d_startZ, MASK_enableAllCaches));\n"
               )
               .append("    ")
               .append("data[index] = result;\n")
               .append("    ")
               .append("if (extra_out) extra_out[index] = result;\n")
               .append("}\n");
            cache2dPrefills.add(namex);
         }

         caches = this.interpolators;
         offset = this.getGlobalDynamicDataOffset(OpenCLCGen.MARKER_cacheLike_interpolator);
         name = caches.object2IntEntrySet().iterator();

         while (name.hasNext()) {
            it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<CacheLikeNode> entry = (it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<CacheLikeNode>)name.next();
            CacheLikeNode node = (CacheLikeNode)entry.getKey();
            int i = entry.getIntValue();
            OpenCLCGen.validateNodeType(node, ReturnType.F64);
            String delegateName = this.newMethodUnoptimized(node.getDelegate(), FunctionVariant.FLATCACHE_ONLY);
            FlowschedAssertions.assertTrue(delegateName != null);
            String namex = "df_interpolator_buffer_prefill_" + i;
            this.pendingSource
               .append("static FUNC_NOINLINE void ")
               .append(namex)
               .append("(global const void * restrict const const_data, global void * restrict const rw_data, global double * restrict const extra_out) {\n")
               .append("    ")
               .append("global const worldgen_params_t * restrict params = rw_data;\n")
               .append("    ")
               .append("global double *data = df_data_offset_global(rw_data, ")
               .append(offset)
               .append(");\n")
               .append("    ")
               .append("int32_t cellRelX = get_global_id(0);\n")
               .append("    ")
               .append("int32_t cellRelY = get_global_id(2);\n")
               .append("    ")
               .append("int32_t cellRelZ = get_global_id(1);\n")
               .append("    ")
               .append("int32_t cellX = cellRelX + params->startCellX;\n")
               .append("    ")
               .append("int32_t cellY = cellRelY + params->startCellY;\n")
               .append("    ")
               .append("int32_t cellZ = cellRelZ + params->startCellZ;\n")
               .append("    ")
               .append("uint32_t index = df_address_interpolator_buffer(params, ")
               .append(i)
               .append(", cellRelX, cellRelY, cellRelZ);\n")
               .append("    ")
               .append("const double result = ")
               .append(delegateName)
               .append(
                  "(make_sample_int32_ctx(const_data, rw_data, cellX * genShapeCfg_horizontalCellBlockCount(), cellY * genShapeCfg_verticalCellBlockCount(), cellZ * genShapeCfg_horizontalCellBlockCount(), MASK_enableFlatCache));\n"
               )
               .append("    ")
               .append("data[index] = result;\n")
               .append("    ")
               .append("if (extra_out) extra_out[index] = result;\n")
               .append("}\n");
            interpolatorPrefills.add(namex);
         }

         if (!flatCachePrefills.isEmpty()) {
            this.pendingSource.append("#ifdef DF_COMPILE_").append(OpenCLCGen.ProgramType.FLAT_CACHE_PREFILL).append("\n");
            int i = 0;

            for (int flatCachePrefillsSize = flatCachePrefills.size(); i < flatCachePrefillsSize; i++) {
               String namex = flatCachePrefills.get(i);
               this.pendingSource
                  .append("kernel __attribute__((reqd_work_group_size(16, 16, 1))) void df_flatcache_prefill_kernel_")
                  .append(i)
                  .append("(global const void * restrict const const_data, global void * restrict const rw_data, global double * restrict const extra_out) {\n")
                  .append("    ")
                  .append("if (!const_data || !rw_data || !extra_out) {\n")
                  .append("    ")
                  .append("    ")
                  .append("#ifdef DEBUG\n")
                  .append("    ")
                  .append("    ")
                  .append(
                     "printf(\"trap: !const_data || !rw_data || !extra_out\\n const_data=%p rw_data=%p extra_out=%p\\n\", const_data, rw_data, extra_out);\n"
                  )
                  .append("    ")
                  .append("    ")
                  .append("#endif\n")
                  .append("    ")
                  .append("    ")
                  .append("__builtin_trap();\n")
                  .append("    ")
                  .append("    ")
                  .append("__builtin_unreachable();\n")
                  .append("    ")
                  .append("    ")
                  .append("return;\n")
                  .append("    ")
                  .append("}\n")
                  .append("    ")
                  .append(namex)
                  .append("(const_data, rw_data, extra_out);\n")
                  .append("}\n");
            }

            this.pendingSource.append("#endif\n");
         }

         if (!cache2dPrefills.isEmpty()) {
            this.pendingSource
               .append("#ifdef DF_COMPILE_")
               .append(OpenCLCGen.ProgramType.CACHE2D_PREFILL)
               .append("\n")
               .append(
                  "kernel __attribute__((reqd_work_group_size(8, 8, 1))) void df_cache2d_prefill_kernel(global const void * restrict const const_data, global void * restrict const rw_data) {\n"
               )
               .append("    ")
               .append("if (!const_data || !rw_data) {\n")
               .append("    ")
               .append("    ")
               .append("#ifdef DEBUG\n")
               .append("    ")
               .append("    ")
               .append("printf(\"trap: !const_data || !rw_data\\n const_data=%p rw_data=%p\\n\", const_data, rw_data);\n")
               .append("    ")
               .append("    ")
               .append("#endif\n")
               .append("    ")
               .append("    ")
               .append("__builtin_trap();\n")
               .append("    ")
               .append("    ")
               .append("__builtin_unreachable();\n")
               .append("    ")
               .append("    ")
               .append("return;\n")
               .append("    ")
               .append("}\n")
               .append("\n");
            int i = 0;

            for (int cache2dPrefillsSize = cache2dPrefills.size(); i < cache2dPrefillsSize; i++) {
               String namex = cache2dPrefills.get(i);
               this.pendingSource
                  .append("    ")
                  .append("if (get_global_id(2) == ")
                  .append(i)
                  .append(") {\n")
                  .append("    ")
                  .append("    ")
                  .append(namex)
                  .append("(const_data, rw_data, NULL);\n")
                  .append("    ")
                  .append("    ")
                  .append("return;\n")
                  .append("    ")
                  .append("}\n");
            }

            this.pendingSource.append("}\n").append("#endif\n");
         }

         if (!interpolatorPrefills.isEmpty()) {
            this.pendingSource
               .append("#ifdef DF_COMPILE_")
               .append(OpenCLCGen.ProgramType.INTERPOLATOR_PREFILL)
               .append("\n")
               .append(
                  "kernel void df_interpolator_buffer_prefill_kernel(global const void * restrict const const_data, global void * restrict const rw_data) {\n"
               )
               .append("    ")
               .append("if (!const_data || !rw_data) {\n")
               .append("    ")
               .append("    ")
               .append("#ifdef DEBUG\n")
               .append("    ")
               .append("    ")
               .append("printf(\"trap: !const_data || !rw_data\\n const_data=%p rw_data=%p\\n\", const_data, rw_data);\n")
               .append("    ")
               .append("    ")
               .append("#endif\n")
               .append("    ")
               .append("    ")
               .append("__builtin_trap();\n")
               .append("    ")
               .append("    ")
               .append("__builtin_unreachable();\n")
               .append("    ")
               .append("    ")
               .append("return;\n")
               .append("    ")
               .append("}\n")
               .append("\n");

            for (String namex : interpolatorPrefills) {
               this.pendingSource.append("    ").append(namex).append("(const_data, rw_data, NULL);\n");
            }

            this.pendingSource.append("}\n").append("#endif\n");
         }
      }

      public void genBiomeTree(BiomeSource biomeSource) {
         if (biomeSource instanceof MultiNoiseBiomeSource multiNoiseBiomeSource) {
            ParameterList<Holder<Biome>> entries = (ParameterList<Holder<Biome>>) reflectBiomeEntries(multiNoiseBiomeSource);
            if (entries != null) {
               RTree<Holder<Biome>> tree = ((IMultiNoiseUtilEntries)entries).getTree();

               int globalOffset;
               int nodeCount;
               int treeDepth;
               NativeBiomeSearchTree nativeBiomeSearchTree = BindingsTemplate.biome_search_tree_node$create(tree);
               byte[] bytes = new byte[nativeBiomeSearchTree.segment.capacity()];
               nativeBiomeSearchTree.segment.get(0, bytes, 0, bytes.length);
               globalOffset = this.allocGlobalConstData(bytes, 8);
               nodeCount = nativeBiomeSearchTree.node_c;
               treeDepth = nativeBiomeSearchTree.tree_depth;
               this.biomeMappings = nativeBiomeSearchTree.biomes;

               this.pendingSource
                  .append("constant const uint32_t biome_multinoise_tree_offset = ")
                  .append(globalOffset)
                  .append(";\n")
                  .append("constant const uint32_t biome_multinoise_tree_nodes_c = ")
                  .append(nodeCount)
                  .append(";\n");
               this.defines.put("BIOME_SEARCH_TREE_MAX_DEPTH", String.valueOf(treeDepth));
               return;
            }
         }

         this.pendingSource
            .append("constant const uint32_t biome_multinoise_tree_offset = 0;\n")
            .append("constant const uint32_t biome_multinoise_tree_nodes_c = 0;\n");
         this.defines.put("BIOME_SEARCH_TREE_MAX_DEPTH", "1");
      }

      public void appendRaw(String raw) {
         this.pendingSource.append(raw);
      }

      private byte[] buildConstData() {
         byte[] constData = new byte[this.globalConstDataTail];
         ObjectIterator var2 = this.globalConstDataOffsets.object2IntEntrySet().iterator();

         while (var2.hasNext()) {
            it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<byte[]> entry = (it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<byte[]>)var2.next();
            byte[] data = (byte[])entry.getKey();
            int offset = entry.getIntValue();
            if (offset + data.length > constData.length) {
               throw new IllegalStateException("Const data offset out of bounds: " + offset + " + " + data.length + " > " + constData.length);
            }

            System.arraycopy(data, 0, constData, offset, data.length);
         }

         return constData;
      }

      public GeneratedCLSource build() {
         FlowschedAssertions.assertTrue(this.localOffsetTableOffset == 0);
         return new GeneratedCLSource(
            OpenCLCGen.ordinal.incrementAndGet(),
            this.pendingSource.toString(),
            this.buildConstData(),
            this.globalDynamicDataOffsets,
            this.flatCaches.size(),
            this.cache2ds.size(),
            this.interpolators.size(),
            this.defines,
            this.biomeMappings,
            null
         );
      }

      private static record FunctionKey(AstNode node, FunctionVariant variant) {
      }
   }

   public static class FunctionContextImpl implements OpenCLCGenFunctionContext {
      private final OpenCLCGen.ContextImpl globalContext;
      private final OpenCLCGen.FunctionContextImpl parent;
      private final StringBuilder pendingBody = new StringBuilder();
      private final Object2ReferenceOpenHashMap<AstNode, String> vars = new Object2ReferenceOpenHashMap();
      private final String methodName;
      private final FunctionVariant variant;
      private int varIdx = 0;

      public FunctionContextImpl(OpenCLCGen.ContextImpl globalContext, OpenCLCGen.FunctionContextImpl parent, String methodName, FunctionVariant variant) {
         this.globalContext = Objects.requireNonNull(globalContext);
         this.parent = parent;
         this.methodName = Objects.requireNonNull(methodName);
         this.variant = Objects.requireNonNull(variant);
      }

      public OpenCLCGenContext getGlobalContext() {
         return this.globalContext;
      }

      public FunctionVariant getVariant() {
         return this.variant;
      }

      public String nextVarName() {
         return this.parent != null ? this.parent.nextVarName() : String.format("var_%d", this.varIdx++);
      }

      public ValuesMethodDef newVar(AstNode node) {
         if (node instanceof ConstantNodeLike constantNodeLike) {
            return constantNodeLike.getDef();
         } else {
            String generated = this.newVarUnoptimized(node);
            return OpenCLCGen.makeValuesMethodDef(generated, node.getReturnType());
         }
      }

      public ValuesMethodDefF64 newVarF64(AstNode node) {
         OpenCLCGen.validateNodeType(node, ReturnType.F64);
         return (ValuesMethodDefF64)this.newVar(node);
      }

      public ValuesMethodDefF32 newVarF32(AstNode node) {
         OpenCLCGen.validateNodeType(node, ReturnType.F32);
         return (ValuesMethodDefF32)this.newVar(node);
      }

      public String newVarUnoptimized(AstNode node) {
         String v;
         String newValue;
         if ((v = this.getVarIfPresent(node)) == null && (newValue = this.newVar0(node)) != null) {
            this.vars.put(node, newValue);
            return newValue;
         } else {
            return v;
         }
      }

      private String newVar0(AstNode node) {
         String varName = this.nextVarName();
         String generated = OpenCLCGenRegistry.doCLGen(node, this, varName);
         this.pendingBody
            .append(OpenCLCGen.getDataType(node.getReturnType()))
            .append(" ")
            .append(varName)
            .append("; // ")
            .append(node.getClass().getName())
            .append("\n")
            .append("{\n")
            .append(generated.indent(4))
            .append("}\n");
         this.globalContext.recordAuxName(node, this.methodName, varName);
         return varName;
      }

      private String getVarIfPresent(AstNode node) {
         String got = (String)this.vars.get(node);
         if (got != null) {
            return got;
         } else {
            if (this.parent != null) {
               got = this.parent.getVarIfPresent(node);
               if (got != null) {
                  this.vars.put(node, got);
                  return got;
               }
            }

            return null;
         }
      }

      public String getDelegateVar(ValuesMethodDef target, ReturnType returnType) {
         OpenCLCGen.validateTarget(target, returnType);
         if (target.isConst()) {
            Objects.requireNonNull(target);

            if (target instanceof ValuesMethodDefF32 f32) {
               return OpenCLCGen.literal(f32.constValue());
            }
            if (target instanceof ValuesMethodDefF64 f64) {
               return OpenCLCGen.literal(f64.constValue());
            }
            throw new IllegalStateException("Unexpected type: " + target.getClass().getName());
         } else {
            return target.generatedMethod();
         }
      }

      public String getDelegateVar(ValuesMethodDefF64 target) {
         return this.getDelegateVar(target, ReturnType.F64);
      }

      public String getDelegateVar(ValuesMethodDefF32 target) {
         return this.getDelegateVar(target, ReturnType.F32);
      }

      public OpenCLCGenFunctionContext fork() {
         return new OpenCLCGen.FunctionContextImpl(this.globalContext, this, this.methodName, this.variant);
      }

      public String getBody() {
         return this.pendingBody.toString();
      }

      public void appendRaw(String raw) {
         this.pendingBody.append(raw);
      }
   }

   public static enum ProgramType {
      ESTIMATE_SURFACE_HEIGHT,
      AQUIFER_PREFILL,
      NOISE_KERNEL,
      FLAT_CACHE_PREFILL,
      CACHE2D_PREFILL,
      INTERPOLATOR_PREFILL,
      BIOME_MULTINOISE_KERNEL;
   }
}
