package com.ishland.c2me.opts.accel.opencl.common.shader_cache;

import com.google.common.base.Stopwatch;
import com.ishland.c2me.opts.accel.opencl.common.compiler.OpenCLCGen;
import com.ishland.c2me.opts.accel.opencl.common.enumeration.OpenCLDeviceMetadata;
import com.ishland.c2me.opts.accel.opencl.common.util.CLUtil;
import com.ishland.c2me.opts.accel.opencl.common.zstd.ZstdInputStreamNoFinalizer;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CL12;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShaderCacheManager {
   private static final Logger LOGGER = LoggerFactory.getLogger(ShaderCacheManager.class);
   private final Map<String, Path> cacheIndex;

   public ShaderCacheManager() {
      Stopwatch stopwatch = Stopwatch.createStarted();

      try {
         this.cacheIndex = Collections.unmodifiableMap(scan());
      } catch (IOException var3) {
         throw new RuntimeException(var3);
      }

      stopwatch.stop();
      LOGGER.info("Indexed {} shader cache entries in {}", this.cacheIndex.size(), stopwatch);
   }

   private static Map<String, Path> scan() throws IOException {
      Path baseDir = Path.of(".", "config", "c2me-shader-delivery");
      if (!Files.isDirectory(baseDir)) {
         return new Object2ObjectOpenHashMap();
      } else {
         Object2ObjectOpenHashMap<String, Path> index = new Object2ObjectOpenHashMap();

         for (Path path : Files.list(baseDir).sorted().toList()) {
            path = path.normalize();
            if (path.getFileName().toString().endsWith(".tar.zst") && Files.isRegularFile(path)) {
               Stopwatch stopwatch = Stopwatch.createStarted();

               try {
                  TarArchiveInputStream in = new TarArchiveInputStream(
                     new BufferedInputStream(new ZstdInputStreamNoFinalizer(Files.newInputStream(path)), 1048576)
                  );

                  TarArchiveEntry entry;
                  try {
                     while ((entry = (TarArchiveEntry) in.getNextEntry()) != null) {
                        String name = entry.getName();
                        if (!name.endsWith("/") && entry.isFile()) {
                           if (index.containsKey(name)) {
                              throw new IllegalStateException(String.format("Duplicate entry in (%s) and (%s): (%s)", path, index.get(name), name));
                           }

                           index.put(name, path);
                        }
                     }
                  } catch (Throwable var9) {
                     try {
                        in.close();
                     } catch (Throwable var8) {
                        var9.addSuppressed(var8);
                     }

                     throw var9;
                  }

                  in.close();
               } catch (IOException var10) {
                  LOGGER.error("Failed to read {}", path, var10);
               }

               stopwatch.stop();
               LOGGER.info("Read {} fully in {}", path, stopwatch);
            }
         }

         return index;
      }
   }

   public void tryCacheDirs(long context, OpenCLDeviceMetadata metadata, EnumMap<OpenCLCGen.ProgramType, Long> map, String... paths) {
      Map<Path, Set<OpenCLCGen.ProgramType>> path2Programs = Arrays.stream(OpenCLCGen.ProgramType.values())
         .flatMap(type -> Arrays.stream(paths).map(path -> Pair.of(type, path)))
         .map(
            pair -> Pair.of(
                  (OpenCLCGen.ProgramType)pair.left(),
                  this.cacheIndex.get(String.format("%s/%s.bin", pair.right(), ((OpenCLCGen.ProgramType)pair.left()).name()))
               )
         )
         .filter(pair -> pair.value() != null)
         .collect(Collectors.groupingBy(Pair::value, Collectors.mapping(Pair::key, Collectors.toSet())));
      if (path2Programs.isEmpty()) {
         LOGGER.info("Cache miss fully for {}", Arrays.toString((Object[])paths));
      }

      for (Entry<Path, Set<OpenCLCGen.ProgramType>> pathEntry : path2Programs.entrySet()) {
         Path archivePath = pathEntry.getKey();
         Set<OpenCLCGen.ProgramType> types = new HashSet<>(pathEntry.getValue());
         tryRead(paths, context, metadata, map, types, archivePath);
      }
   }

   private static void tryRead(
      String[] paths,
      long context,
      OpenCLDeviceMetadata metadata,
      EnumMap<OpenCLCGen.ProgramType, Long> map,
      Set<OpenCLCGen.ProgramType> types,
      Path archivePath
   ) {
      LOGGER.info("Loading programs {} from archive {}", Arrays.toString(types.toArray(OpenCLCGen.ProgramType[]::new)), archivePath);
      Stopwatch stopwatch = Stopwatch.createStarted();

      try {
         TarArchiveInputStream in = new TarArchiveInputStream(
            new BufferedInputStream(new ZstdInputStreamNoFinalizer(Files.newInputStream(archivePath)), 1048576)
         );

         TarArchiveEntry entry;
         try {
            while ((entry = (TarArchiveEntry) in.getNextEntry()) != null) {
               String name = entry.getName();
               if (!name.endsWith("/") && entry.isFile() && name.endsWith(".bin") && !Arrays.stream(paths).noneMatch(name::startsWith)) {
                  if (!in.canReadEntryData(entry)) {
                     LOGGER.error("Unable to read {} from archive {}, something is wrong", name, archivePath);
                  } else {
                     OpenCLCGen.ProgramType programType;
                     try {
                        programType = OpenCLCGen.ProgramType.valueOf(name.substring(name.lastIndexOf(47) + 1, name.length() - ".bin".length()));
                     } catch (IllegalArgumentException var15) {
                        LOGGER.warn("Illegal filename: {}", name);
                        continue;
                     }

                     if (!types.contains(programType)) {
                        LOGGER.warn("Ignoring {} from archive {} because it is not in the index", name, archivePath);
                     } else {
                        LOGGER.info("Attempting to load cache for {}: {}!{}", new Object[]{programType, archivePath, name});
                        long program = tryLoadBinary(context, metadata, in.readAllBytes());
                        if (program != 0L) {
                           LOGGER.info("Cache hit for {}: {}!{}", new Object[]{programType, archivePath, name});
                           map.put(programType, program);
                           types.remove(programType);
                        }

                        if (types.isEmpty()) {
                           break;
                        }
                     }
                  }
               }
            }
         } catch (Throwable var16) {
            try {
               in.close();
            } catch (Throwable var14) {
               var16.addSuppressed(var14);
            }

            throw var16;
         }

         in.close();
      } catch (IOException var17) {
         LOGGER.error("Failed to read {}", archivePath, var17);
      }

      stopwatch.stop();
      LOGGER.info("Read {} in {}", archivePath, stopwatch);
   }

   public byte[] tryCache(String path) {
      Path archivePath = this.cacheIndex.get(path);
      if (archivePath == null) {
         return null;
      } else {
         try {
            TarArchiveInputStream in = new TarArchiveInputStream(
               new BufferedInputStream(new ZstdInputStreamNoFinalizer(Files.newInputStream(archivePath)), 1048576)
            );

            byte[] var6;
            label52: {
               Object var10;
               try {
                  TarArchiveEntry entry;
                  while ((entry = (TarArchiveEntry) in.getNextEntry()) != null) {
                     String name = entry.getName();
                     if (!name.endsWith("/") && entry.isFile() && name.equals(path)) {
                        if (in.canReadEntryData(entry)) {
                           var6 = in.readAllBytes();
                           break label52;
                        }

                        LOGGER.error("Unable to read {} from archive {}, something is wrong", path, archivePath);
                     }
                  }

                  var10 = null;
               } catch (Throwable var8) {
                  try {
                     in.close();
                  } catch (Throwable var7) {
                     var8.addSuppressed(var7);
                  }

                  throw var8;
               }

               in.close();
               return (byte[])var10;
            }

            in.close();
            return var6;
         } catch (IOException var9) {
            LOGGER.error("Failed to read {}", archivePath, var9);
            return null;
         }
      }
   }

   private static long tryLoadBinary(long context, OpenCLDeviceMetadata metadata, byte[] binary) {
      try {
         MemoryStack stack = MemoryStack.stackPush();

         long var19;
         label71: {
            long var12;
            label72: {
               try {
                  IntBuffer errorCodeRet = stack.callocInt(1);
                  ByteBuffer byteBuffer = MemoryUtil.memAlloc(binary.length);
                  byteBuffer.put(0, binary).rewind();
                  PointerBuffer devicePtrBuffer = (PointerBuffer)stack.mallocPointer(1).put(0, metadata.devicePtr).rewind();
                  IntBuffer binErrorCodeRet = stack.callocInt(1);
                  long program = CL12.clCreateProgramWithBinary(context, devicePtrBuffer, byteBuffer, binErrorCodeRet, errorCodeRet);
                  MemoryUtil.memFree(byteBuffer);
                  if (errorCodeRet.get(0) == -42) {
                     LOGGER.info("Shader binary loading failed: CL_INVALID_BINARY");
                     var19 = 0L;
                     break label71;
                  }

                  CLUtil.checkCLError(errorCodeRet);

                  try {
                     CLUtil.checkCLError(binErrorCodeRet);
                     int errcode = CL12.nclBuildProgram(program, 1, MemoryUtil.memAddress(devicePtrBuffer), 0L, 0L, 0L);
                     if (errcode == -11) {
                        LOGGER.info("Shader binary loading failed: CL_BUILD_PROGRAM_FAILURE");
                        CLUtil.checkCLError(CL12.clReleaseProgram(program));
                        var12 = 0L;
                        break label72;
                     }

                     CLUtil.checkCLError(errcode);
                  } catch (Throwable var15) {
                     CLUtil.checkCLError(CL12.clReleaseProgram(program));
                     throw var15;
                  }

                  var19 = program;
               } catch (Throwable var16) {
                  if (stack != null) {
                     try {
                        stack.close();
                     } catch (Throwable var14) {
                        var16.addSuppressed(var14);
                     }
                  }

                  throw var16;
               }

               if (stack != null) {
                  stack.close();
               }

               return var19;
            }

            if (stack != null) {
               stack.close();
            }

            return var12;
         }

         if (stack != null) {
            stack.close();
         }

         return var19;
      } catch (Throwable var17) {
         LOGGER.error("Failed to load compilation cache", var17);
         return 0L;
      }
   }
}
