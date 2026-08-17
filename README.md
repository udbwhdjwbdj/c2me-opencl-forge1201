# C2ME + OpenCL Acceleration — Minecraft 1.20.1 Forge Port

> **AI-assisted port**: This project was reverse-engineered with the assistance of DeepSeek. The source code is provided for educational and research purposes only.

A full source project that ports **C2ME** (Concurrent Chunk Management Engine) and the **C2ME OpenCL Acceleration Module** to **Minecraft 1.20.1 Forge**, integrated into a single mod.

[中文版](README.zh-CN.md)

## 🙏 Acknowledgements

This port would not exist without the original projects. Our sincere gratitude goes to all the authors and contributors of **C2ME (Fabric)**, **C2ME (NeoForge)** and the **c2meF** 1.20.1 Forge port — the incredible engineering work behind chunk performance optimizations and GPU-accelerated world generation. We deeply respect and thank everyone involved.

## 📦 What's in this repo

Two modules ported from the 1.21.1 NeoForge builds:

| Module | modid | Description |
|---|---|---|
| c2me_opts_dfc | c2me_opts_dfc | Density Function Compiler: converts Minecraft density functions into an AST and optimizes them for OpenCL kernel generation (hard prerequisite of the OpenCL module) |
| c2me_opts_accel_opencl | c2me_opts_accel_opencl | OpenCL acceleration: batch-executes world generation on the GPU (biome multinoise + block density), including device enumeration, CL program compilation, shader cache and vendor workarounds |

These work together with **c2meF** (the unofficial 1.20.1 Forge port containing 22 modules). The released jar packages all 24 modules into a single mod.

## 🔨 Building

Requirements: JDK 17, Gradle 8.8 (or compatible 8.x), network access to Maven Central / maven.minecraftforge.net / repo.spongepowered.org.

```bash
gradle :c2me_opts_dfc:reobfJar :c2me_opts_accel_opencl:reobfJar
```

Artifacts:
- `c2me_opts_dfc/build/libs/c2meF-opts-dfc-mc1.20.1-*.jar`
- `c2me_opts_accel_opencl/build/libs/c2meF-opts-accel-opencl-mc1.20.1-*.jar`

### Dependencies

- `../libs/*.jar`: reobf jars of the c2meF modules (c2meF-base, etc.), plus lwjgl-opencl / lwjgl-zstd / caffeine.
- The OpenCL module calls the system OpenCL.dll directly — no extra LWJGL natives required.
- Release packaging: place both module jars into `META-INF/jarjar/` of the c2meF-all.jar and update `metadata.json`.

## 🔧 Porting notes (1.21.1 NeoForge → 1.20.1 Forge)

1. **API adaptation**: The `DensityFunction` API is identical between 1.20.1 and 1.21.1 (compute/fillArray/mapAll). Main differences: `NoiseChunk` (fields cellWidth/cellHeight/beardifier), `ChunkMap` (use the generator field during construction), `Climate.RTree` (1.20.1 exposes no public accessors — solved via mixin accessors).
2. **Java version**: Pure Java 17 standard syntax (no preview features), compatible with both Java 17 and Java 21 runtimes.
3. **Mixin remapping**: Uses mixin accessors + refmap for SRG runtime mapping. (AT member lines require SRG names under Forge official mappings, which is unreliable.)
4. **Chunk-system integration trimmed**: The 1.21.1 chunk-system rewrite (BatchingBiomeNoiseStatus) does not exist in 1.20.1; OpenCL hooks in via codegen at `ChunkMap` construction and `NoiseBasedChunkGenerator.doFill` interception.

## ⚙️ Configuration

Runtime config lives in `config/c2me-*.toml` (C2ME ConfigSystem):

- `openclAccel.enabled`: master switch for OpenCL acceleration (default true)
- `openclAccel.allowIncompatibilityFallback`: fall back to vanilla generation when OpenCL init fails (recommended true)
- `openclAccel.allowCPUDevices` / `allowGPUDevices`: device type filters

## 📜 License

- c2meF (c2me Forge port): MIT (upstream RelativityMC)
- dfc / OpenCL modules: reverse-engineered from the closed-source NeoForge jars — provided for learning and research only, **not for commercial redistribution**.
