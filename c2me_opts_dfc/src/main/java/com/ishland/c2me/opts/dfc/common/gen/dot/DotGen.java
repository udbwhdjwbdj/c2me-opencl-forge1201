package com.ishland.c2me.opts.dfc.common.gen.dot;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.ConstantNodeLike;
import com.ishland.c2me.opts.dfc.common.ast.misc.CoordinateNode;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.util.CubicSpline;
import net.minecraft.world.level.levelgen.DensityFunctions.Spline.Coordinate;
import net.minecraft.world.level.levelgen.DensityFunctions.Spline.Point;

public class DotGen {
   private static final AtomicInteger ID = new AtomicInteger();

   public static class Context {
      private final Object2ReferenceOpenHashMap<AstNode, DotGen.Context.Builder.Impl> allocatedNodes = new Object2ReferenceOpenHashMap();
      private final Object2ReferenceOpenHashMap<CubicSpline<Point, Coordinate>, DotGen.Context.Builder.Impl> allocatedSplines = new Object2ReferenceOpenHashMap();
      private final ReferenceArrayList<DotGen.Context.Builder.Impl> constants = new ReferenceArrayList();
      private final ReferenceArrayList<DotGen.Context.Builder.Impl> extras = new ReferenceArrayList();
      private final Function<Object, String> auxNameProvider;
      private int counter = 0;

      public Context(Function<Object, String> auxNameProvider) {
         this.auxNameProvider = auxNameProvider != null ? auxNameProvider : o -> null;
      }

      public static String base26(int id) {
         StringBuilder builder = new StringBuilder(2);

         do {
            builder.insert(0, (char)(97 + id % 26));
         } while ((id /= 26) > 0);

         return builder.toString();
      }

      void reset() {
         this.allocatedNodes.clear();
         this.constants.clear();
         this.counter = 0;
      }

      public int generate(AstNode node) {
         DotGen.Context.Builder.Impl builder;
         if (!(node instanceof ConstantNodeLike) && !(node instanceof CoordinateNode)) {
            builder = (DotGen.Context.Builder.Impl)this.allocatedNodes
               .computeIfAbsent(node, node1 -> new DotGen.Context.Builder.Impl(this.counter++, this.auxNameProvider.apply(node1)));
            if (builder.frozen) {
               return builder.id;
            }
         } else {
            builder = new DotGen.Context.Builder.Impl(this.counter++, null);
            this.constants.add(builder);
         }

         return DotGenRegistry.doDotGen(node, this, builder);
      }

      public DotGen.Context.Builder getSplineBuilder(CubicSpline<Point, Coordinate> spline) {
         return (DotGen.Context.Builder.Impl)this.allocatedSplines
            .computeIfAbsent(spline, spline1 -> new DotGen.Context.Builder.Impl(this.counter++, this.auxNameProvider.apply(spline1)));
      }

      public DotGen.Context.Builder createExtraBuilder() {
         DotGen.Context.Builder.Impl builder = new DotGen.Context.Builder.Impl(this.counter++, null);
         this.extras.add(builder);
         return builder;
      }

      public String write(String name, int rootId) {
         Iterator<DotGen.Context.Builder.Impl> iterator = Stream.concat(
               Stream.concat(this.constants.stream(), this.allocatedNodes.values().stream()),
               Stream.concat(this.allocatedSplines.values().stream(), this.extras.stream())
            )
            .sorted(Comparator.comparing(DotGen.Context.Builder.Impl::getId))
            .iterator();
         StringBuilder sb = new StringBuilder();
         sb.append("strict digraph ").append(name).append(" {");
         sb.append('\n');
         sb.append('\t').append("root=").append(base26(rootId));
         sb.append("\n\t").append("ranksep=1");

         while (iterator.hasNext()) {
            sb.append('\n');
            iterator.next().write(sb);
         }

         sb.append('\n');
         sb.append("}");
         return sb.toString();
      }

      public sealed interface Builder permits DotGen.Context.Builder.Impl {
         DotGen.Context.Builder boxShape();

         DotGen.Context.Builder diamondShape();

         DotGen.Context.Builder circleShape();

         DotGen.Context.Builder ovalShape();

         DotGen.Context.Builder triangleShape();

         DotGen.Context.Builder invTriangleShape();

         DotGen.Context.Builder trapeziumShape();

         DotGen.Context.Builder hexagonShape();

         DotGen.Context.Builder cdsShape();

         DotGen.Context.Builder folderShape();

         DotGen.Context.Builder parallelogramShape();

         DotGen.Context.Builder label(String var1);

         DotGen.Context.Builder tooltip(String var1);

         DotGen.Context.Builder.Edge edge(int var1);

         int build();

         boolean isFrozen();

         int getId();

         void write(StringBuilder var1);

         public sealed interface Edge permits DotGen.Context.Builder.Impl.EdgeBuilder {
            DotGen.Context.Builder.Edge label(String var1);

            DotGen.Context.Builder.Edge color(String var1);

            DotGen.Context.Builder finish();

            @Override
            String toString();
         }

         public static final class Impl implements DotGen.Context.Builder {
            private final ReferenceArrayList<DotGen.Context.Builder.Impl.EdgeBuilder> edges = new ReferenceArrayList();
            private String shape;
            private String label;
            private String tooltip;
            private boolean frozen = false;
            private final int id;
            private final String auxName;

            Impl(int id, String auxName) {
               this.id = id;
               this.auxName = auxName;
            }

            @Override
            public DotGen.Context.Builder boxShape() {
               this.shape = "box";
               return this;
            }

            @Override
            public DotGen.Context.Builder diamondShape() {
               this.shape = "diamond";
               return this;
            }

            @Override
            public DotGen.Context.Builder circleShape() {
               this.shape = "circle";
               return this;
            }

            @Override
            public DotGen.Context.Builder ovalShape() {
               this.shape = "oval";
               return this;
            }

            @Override
            public DotGen.Context.Builder triangleShape() {
               this.shape = "triangle";
               return this;
            }

            @Override
            public DotGen.Context.Builder invTriangleShape() {
               this.shape = "invtriangle";
               return this;
            }

            @Override
            public DotGen.Context.Builder trapeziumShape() {
               this.shape = "trapezium";
               return this;
            }

            @Override
            public DotGen.Context.Builder hexagonShape() {
               this.shape = "hexagon";
               return this;
            }

            @Override
            public DotGen.Context.Builder cdsShape() {
               this.shape = "cds";
               return this;
            }

            @Override
            public DotGen.Context.Builder folderShape() {
               this.shape = "folder";
               return this;
            }

            @Override
            public DotGen.Context.Builder parallelogramShape() {
               this.shape = "parallelogram";
               return this;
            }

            @Override
            public DotGen.Context.Builder label(String label) {
               this.label = label;
               return this;
            }

            @Override
            public DotGen.Context.Builder tooltip(String tooltip) {
               this.tooltip = tooltip;
               return this;
            }

            @Override
            public DotGen.Context.Builder.Edge edge(int from) {
               DotGen.Context.Builder.Impl.EdgeBuilder edge = new DotGen.Context.Builder.Impl.EdgeBuilder(from);
               this.edges.add(edge);
               return edge;
            }

            @Override
            public int build() {
               this.frozen = true;
               return this.id;
            }

            @Override
            public boolean isFrozen() {
               return this.frozen;
            }

            @Override
            public int getId() {
               return this.id;
            }

            @Override
            public void write(StringBuilder sb) {
               String name = DotGen.Context.base26(this.id);
               sb.append("\t").append(name).append(" [shape=").append(this.shape).append(", ");
               if (this.label != null && this.label.startsWith("<") && this.label.endsWith(">")) {
                  sb.append("label=").append(this.label);
               } else {
                  sb.append("label=\"");
                  sb.append("id=").append(name).append("\\n");
                  if (this.auxName != null) {
                     sb.append("auxName=").append(this.auxName).append("\\n");
                  }

                  sb.append(this.label).append("\"");
               }

               sb.append(", tooltip=\"");
               if (this.tooltip != null) {
                  sb.append(this.tooltip).append("\\n");
               }

               ObjectListIterator var3 = this.edges.iterator();

               while (var3.hasNext()) {
                  DotGen.Context.Builder.Impl.EdgeBuilder edge = (DotGen.Context.Builder.Impl.EdgeBuilder)var3.next();
                  String node = DotGen.Context.base26(edge.from);
                  if (edge.label != null) {
                     sb.append(edge.label).append(".id=").append(node).append("\\n");
                  } else {
                     sb.append("children[].id=").append(node).append("\\n");
                  }
               }

               sb.append('"');
               sb.append("]");
               var3 = this.edges.iterator();

               while (var3.hasNext()) {
                  DotGen.Context.Builder.Impl.EdgeBuilder edge = (DotGen.Context.Builder.Impl.EdgeBuilder)var3.next();
                  sb.append('\n');
                  sb.append("\t");
                  String node = DotGen.Context.base26(edge.from);
                  sb.append(node).append(" -> ").append(name).append(edge);
               }
            }

            final class EdgeBuilder implements DotGen.Context.Builder.Edge {
               private final int from;
               private String label;
               private String color;

               EdgeBuilder(int from) {
                  this.from = from;
               }

               @Override
               public DotGen.Context.Builder.Edge label(String label) {
                  this.label = label;
                  return this;
               }

               @Override
               public DotGen.Context.Builder.Edge color(String color) {
                  this.color = color;
                  return this;
               }

               public DotGen.Context.Builder.Impl finish() {
                  return Impl.this;
               }

               @Override
               public String toString() {
                  if (this.label == null && this.color == null) {
                     return "";
                  } else if (this.label == null) {
                     return " [color=" + this.color + "]";
                  } else {
                     return this.color == null ? " [label=\"" + this.label + "\"]" : " [label=\"" + this.label + "\", color=" + this.color + "]";
                  }
               }
            }
         }
      }
   }
}
