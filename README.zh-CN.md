# C2ME + OpenCL 加速 —— Minecraft 1.20.1 Forge 移植版

> **AI 辅助移植**：本项目由 AI 编码助手（DeepSeek）辅助完成逆向与移植，源码仅供学习研究。

将 **C2ME**（Concurrent Chunk Management Engine）与 **C2ME OpenCL Acceleration Module** 移植并整合到 **Minecraft 1.20.1 Forge** 的完整源码工程。

[English](README.md)

## 🙏 致谢

本项目的诞生离不开以下三个原版模组，在此由衷感谢各位作者的卓越工程贡献：

| 项目 | 作者 | 贡献 |
|---|---|---|
| [C2ME (Fabric)](https://github.com/RelativityMC/C2ME-fabric) | RelativityMC、ishland | C2ME 原始实现 |
| [C2ME NeoForge](https://github.com/RelativityMC/C2ME-neoforge) | RelativityMC、ishland | NeoForge 移植版，本移植 OpenCL 加速模块的源码来源 |
| c2meF（非官方 1.20.1 Forge 移植） | sjhub | 本移植所依赖的 1.20.1 Forge 基础 |

感谢他们让区块性能优化与 GPU 加速世界生成成为可能。

## 📦 仓库内容

本仓库包含从 1.21.1 NeoForge 移植的两个模块：

| 模块 | modid | 说明 |
|---|---|---|
| c2me_opts_dfc | c2me_opts_dfc | Density Function Compiler：将 MC 密度函数 AST 化并优化，供 OpenCL 生成 GPU 内核（OpenCL 模块的硬前置） |
| c2me_opts_accel_opencl | c2me_opts_accel_opencl | OpenCL 加速：在 GPU 上批量执行世界生成（生物群系多噪声 + 方块密度），含设备枚举、CL 程序编译、着色器缓存与各厂商 workarounds |

与 **c2meF**（非官方 1.20.1 Forge 移植版，含 22 个模块）配合使用。发布 jar 将全部 24 个模块整合为单个 mod。

## 🔨 构建

环境要求：JDK 17、Gradle 8.8（或兼容 8.x）、可访问 Maven Central / maven.minecraftforge.net / repo.spongepowered.org。

```bash
gradle :c2me_opts_dfc:reobfJar :c2me_opts_accel_opencl:reobfJar
```

产物：
- `c2me_opts_dfc/build/libs/c2meF-opts-dfc-mc1.20.1-*.jar`
- `c2me_opts_accel_opencl/build/libs/c2meF-opts-accel-opencl-mc1.20.1-*.jar`

### 依赖说明

- `../libs/*.jar`：c2meF 各模块的 reobf jar（c2meF-base 等 20+ 个），以及 lwjgl-opencl / lwjgl-zstd / caffeine。
- OpenCL 模块直接调用系统 OpenCL.dll（GPU 驱动提供），无需额外 LWJGL native。
- 发布整合：将两个模块 jar 放入 c2meF-all.jar 的 `META-INF/jarjar/` 并更新 `metadata.json`。

## 🔧 移植要点（1.21.1 NeoForge → 1.20.1 Forge）

1. **API 适配**：1.20.1 与 1.21.1 的 `DensityFunction` API 相同（compute/fillArray/mapAll）。主要差异：`NoiseChunk`（字段 cellWidth/cellHeight/beardifier）、`ChunkMap`（构造期使用 generator 字段）、`Climate.RTree`（1.20.1 无公开访问器，改用 mixin access 解决）。
2. **Java 版本**：纯 Java 17 标准语法编译（无 preview 特性），兼容 Java 17/21 运行时。
3. **mixin 映射**：通过 mixin access + refmap 解决 srg 运行时映射（Forge 官方映射下 AT 成员行需写 srg 名，不可靠）。
4. **chunk-system 集成裁剪**：1.21.1 的新 chunk system（BatchingBiomeNoiseStatus）在 1.20.1 不存在，OpenCL 通过 `ChunkMap` 构造期 codegen 与 `NoiseBasedChunkGenerator.doFill` 拦截接入。

## ⚙️ 配置

运行时配置位于 `config/c2me-*.toml`（C2ME ConfigSystem）：

- `openclAccel.enabled`：OpenCL 加速总开关（默认 true）
- `openclAccel.allowIncompatibilityFallback`：OpenCL 初始化失败时回退原版生成（建议 true）
- `openclAccel.allowCPUDevices` / `allowGPUDevices`：设备类型过滤

## 📜 许可

- c2meF（c2me Forge 移植）：MIT（上游 RelativityMC）
- dfc / OpenCL 模块：由闭源 NeoForge jar 逆向移植，仅供学习研究，**禁止商业分发**。
