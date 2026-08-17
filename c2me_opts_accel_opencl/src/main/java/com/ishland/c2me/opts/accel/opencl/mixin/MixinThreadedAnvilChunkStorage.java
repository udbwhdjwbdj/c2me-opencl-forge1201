package com.ishland.c2me.opts.accel.opencl.mixin;

import com.google.common.base.Stopwatch;
import com.ishland.c2me.opts.accel.opencl.common.Config;
import com.ishland.c2me.opts.accel.opencl.common.compiler.GeneratedCLSource;
import com.ishland.c2me.opts.accel.opencl.common.compiler.OpenCLCGen;
import com.ishland.c2me.opts.accel.opencl.common.compiler.emitters.misc.CLBlockStateMappings;
import com.ishland.c2me.opts.accel.opencl.common.ducks.MinecraftServerExtension;
import com.ishland.c2me.opts.accel.opencl.common.ducks.TACSExtension;
import com.ishland.c2me.opts.accel.opencl.common.gen.CLServerGlobalContext;
import com.ishland.c2me.opts.accel.opencl.common.gen.CLServerWorldContext;
import com.ishland.c2me.opts.dfc.common.ast.opto.OptoPasses.AstPair;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ChunkMap.class})
public class MixinThreadedAnvilChunkStorage implements TACSExtension {
   @Shadow
   @Final
   private ServerLevel level;
   @Shadow
   @Final
   private RandomState randomState;
   @Shadow
   @Final
   private static Logger LOGGER;
   @Shadow
   private ChunkGenerator generator;
   @Unique
   private CLServerWorldContext c2me$clContext;

   @Inject(
      method = {"<init>"},
      at = {@At("RETURN")}
   )
   private void postInit(CallbackInfo ci) {
      ChunkGenerator generator = this.generator;
      NoiseGeneratorSettings settings;
      if (generator instanceof NoiseBasedChunkGenerator noiseChunkGenerator) {
         settings = (NoiseGeneratorSettings)noiseChunkGenerator.generatorSettings().value();
      } else {
         settings = NoiseGeneratorSettings.dummy();
      }

      NoiseSettings trimmed = settings.noiseSettings().clampToHeightAccessor(this.level);
      Reference2ReferenceMap<DensityFunction, AstPair> optoCache = new Reference2ReferenceOpenHashMap();
      NoiseRouter originalNoiseRouter = this.randomState.router();
      Stopwatch compilationStopwatch = Stopwatch.createStarted();

      GeneratedCLSource generatedCLSource;
      try {
         generatedCLSource = OpenCLCGen.compile(
            originalNoiseRouter, trimmed, optoCache, originalNoiseRouter.finalDensity(), generator.getBiomeSource()
         );
      } catch (Throwable var10) {
         LOGGER.error("OpenCL codegen for world {} failed", this.level.dimension().location(), var10);
         if (!Config.allowIncompatibilityFallback) {
            throw new RuntimeException("OpenCL codegen failed", var10);
         }

         generatedCLSource = null;
      }

      compilationStopwatch.stop();
      LOGGER.info("OpenCL codegen for world {} finished in {}", this.level.dimension().location(), compilationStopwatch);
      CLServerGlobalContext globalContext = ((MinecraftServerExtension)this.level.getServer()).c2me$getCLContext();
      if (globalContext == null) {
         LOGGER.warn("World {} cannot use OpenCL since the global context is not initialized", this.level.dimension().location());
         if (!Config.allowIncompatibilityFallback) {
            throw new IllegalStateException("OpenCL global context is not initialized");
         }
      } else if (generatedCLSource == null) {
         LOGGER.warn("World {} does not have compiled CL code. Is it incompatible with CL?", this.level.dimension().location());
         if (!Config.allowIncompatibilityFallback) {
            throw new IllegalStateException("OpenCL codegen failed");
         }
      } else {
         LOGGER.info(
            "Source size: {} bytes, const_data size: {} bytes", generatedCLSource.getGeneratedSource().length(), generatedCLSource.getConstData().length
         );
         this.c2me$clContext = new CLServerWorldContext(
            globalContext,
            this.level.dimension().location().toString(),
            generatedCLSource,
            trimmed,
            CLBlockStateMappings.defaultMappings(settings.defaultBlock(), settings.defaultFluid())
         );
      }
   }

   @Inject(
      method = {"close"},
      at = {@At("RETURN")}
   )
   private void postClose(CallbackInfo ci) {
      if (this.c2me$clContext != null) {
         this.c2me$clContext.releaseAllDevices();
         this.c2me$clContext = null;
      }
   }

   @Override
   public CLServerWorldContext c2me$getCLContext() {
      return this.c2me$clContext;
   }
}
