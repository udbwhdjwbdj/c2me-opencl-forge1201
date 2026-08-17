package com.ishland.c2me.opts.dfc.common.gen;

import com.google.common.io.Files;
import com.ishland.c2me.opts.dfc.common.ast.opto.OptoPasses;
import com.ishland.c2me.opts.dfc.common.gen.dot.DotGen;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

public class GenDumper {
   public static final File exportDir = new File("./cache/c2me-dfc");

   private static void deleteRecursively(java.io.File file) {
      java.io.File[] children = file.listFiles();
      if (children != null) {
         for (java.io.File child : children) {
            if (child.isDirectory()) {
               deleteRecursively(child);
            } else {
               child.delete();
            }
         }
      }
      file.delete();
   }

   public static Path dumpClass(String className, byte[] bytes) {
      File outputFile = new File(exportDir, "classes/" + className + ".class");
      outputFile.getParentFile().mkdirs();

      try {
         Files.write(bytes, outputFile);
      } catch (IOException var4) {
         var4.printStackTrace();
      }

      return outputFile.getAbsoluteFile().toPath();
   }

   public static Path dumpCL(String name, byte[] bytes) {
      File outputFile = new File(exportDir, "cl/" + name + ".cl");
      outputFile.getParentFile().mkdirs();

      try {
         Files.write(bytes, outputFile);
      } catch (IOException var4) {
         var4.printStackTrace();
      }

      return outputFile.getAbsoluteFile().toPath();
   }

   public static void dumpDot(String name, Path primary, Map<String, OptoPasses.AstPair> roots, Function<Object, String> auxNameProvider) {
      DotGen.Context ctx = new DotGen.Context(null);
      DotGen.Context.Builder rootNode = ctx.createExtraBuilder();
      rootNode.boxShape().label("Start");

      for (Entry<String, OptoPasses.AstPair> entry : roots.entrySet()) {
         DotGen.Context.Builder entryBuilder = ctx.createExtraBuilder();
         int entryNode = entryBuilder.boxShape().label(entry.getKey()).edge(ctx.generate(entry.getValue().tryUnoptimized())).label("delegate").finish().build();
         rootNode.edge(entryNode).label(entry.getKey()).finish();
      }

      int rootId = rootNode.build();
      String content = ctx.write(name + "_unoptimized", rootId);

      try {
         Files.write(content.getBytes(StandardCharsets.UTF_8), primary.getParent().resolve(primary.getFileName().toString() + ".unoptimized.gv").toFile());
      } catch (IOException var11) {
         var11.printStackTrace();
      }

      ctx = new DotGen.Context(auxNameProvider);
      rootNode = ctx.createExtraBuilder();
      rootNode.boxShape().label("Start");

      for (Entry<String, OptoPasses.AstPair> entry : roots.entrySet()) {
         DotGen.Context.Builder entryBuilder = ctx.createExtraBuilder();
         int entryNode = entryBuilder.boxShape().label(entry.getKey()).edge(ctx.generate(entry.getValue().optimized())).label("delegate").finish().build();
         rootNode.edge(entryNode).label(entry.getKey()).finish();
      }

      rootId = rootNode.build();
      content = ctx.write(name, rootId);

      try {
         Files.write(content.getBytes(StandardCharsets.UTF_8), primary.getParent().resolve(primary.getFileName().toString() + ".gv").toFile());
      } catch (IOException var10) {
         var10.printStackTrace();
      }
   }

   static {
      try {
         deleteRecursively(exportDir);
      } catch (Throwable var1) {
         var1.printStackTrace();
      }
   }
}
