# C2ME + OpenCL Acceleration - Minecraft 1.20.1 Forge 移植版

将 **C2ME**（Concurrent Chunk Management Engine）与 **C2ME OpenCL Acceleration Module** 移植并整合到 **Minecraft 1.20.1 Forge** 的完整源码工程。

## 简介

本仓库包含两个移植自 1.21.1 NeoForge 的模块（从闭源 jar 反编译移植）：

| 模块 | modid | 说明 |
|---|---|---|
| c2me_opts_dfc | c2me_opts_dfc | Density Function Compiler：将 MC 的密度函数 AST 化、优化，供 OpenCL 生成 GPU 内核（OpenCL 模块的硬前置） |
| c2me_opts_accel_opencl | c2me_opts_accel_opencl | OpenCL 加速：在 GPU 上批量执行世界生成（生物群系噪声 + 方块密度），含设备枚举、CL 程序编译、shader 缓存、厂商 workarounds |

配合 **c2meF**（非官方 1.20.1 Forge 移植的 c2me，含 22 个模块）一起使用。整合后的发布 jar 将全部 24 个模块打包为一个 mod。

## 构建

需要：JDK 17、Gradle 8.8（或兼容 8.x）、网络（Maven Central / maven.minecraftforge.net / repo.spongepowered.org）。

```bash
# 需要先把 c2meF 的模块 jar 放入 c2me_opts_accel_opencl 的依赖目录
# 见下方"依赖说明"
gradle :c2me_opts_dfc:reobfJar :c2me_opts_accel_opencl:reobfJar
```

产物：
- `c2me_opts_dfc/build/libs/c2meF-opts-dfc-mc1.20.1-*.jar`
- `c2me_opts_accel_opencl/build/libs/c2meF-opts-accel-opencl-mc1.20.1-*.jar`

### 依赖说明

- `../libs/*.jar`：c2meF 各模块的 reobf jar（c2meF-base 等 20+ 个），以及 lwjgl-opencl / lwjgl-zstd / caffeine 的 jar。
- OpenCL 模块运行时需要 `lwjgl-opencl`（直接调用系统 OpenCL.dll，无需额外 native）。
- 整合发布：把两个模块 jar 放入 c2meF-all.jar 的 `META-INF/jarjar/` 并更新 `metadata.json`（见发布流程）。

## 移植要点（从 1.21.1 NeoForge 到 1.20.1 Forge）

1. **API 适配**：1.20.1 与 1.21.1 的 `DensityFunction` API 相同（compute/fillArray/mapAll），主要差异在 `NoiseChunk`（字段 cellWidth/cellHeight/beardifier）、`ChunkMap`（构造期 generator 字段）、`Climate.RTree`（1.20.1 无公开访问器，改用 mixin access）。
2. **Java 版本**：模块用纯 Java 17 标准语法编译（无 preview），兼容 Java 17/21 运行时。
3. **mixin 映射**：通过 mixin access + refmap 解决 srg 运行时映射（不依赖 AT 成员行，AT 成员行在 Forge 官方映射下需 srg 名，不可靠）。
4. **chunk-system 集成裁剪**：1.21.1 的新 chunk system（BatchingBiomeNoiseStatus）在 1.20.1 不存在，OpenCL 以 `ChunkMap` 构造期 codegen + `NoiseBasedChunkGenerator.doFill` 拦截方式接入。

## 配置

运行时配置在 `config/c2me-*.toml`（C2ME ConfigSystem）：

- `openclAccel.enabled`：OpenCL 加速总开关（默认 true）
- `openclAccel.allowIncompatibilityFallback`：初始化失败时回退原版生成（建议 true）
- `openclAccel.allowCPUDevices` / `allowGPUDevices`：设备类型过滤

## 许可

- c2meF（c2me Forge 移植）：MIT（上游 RelativityMC）
- dfc / OpenCL 模块：由闭源 NeoForge jar 反编译移植，仅供学习研究，请勿用于商业分发。

## 致谢

- [C2ME](https://github.com/RelativityMC/C2ME-fabric)（RelativityMC / ishland）
- c2meF 1.20.1 Forge 移植（sjhub）
