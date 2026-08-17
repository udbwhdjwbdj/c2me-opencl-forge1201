package com.ishland.c2me.opts.dfc.common.ast.spline;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.AstTransformer;
import com.ishland.c2me.opts.dfc.common.ast.McToAst;
import com.ishland.c2me.opts.dfc.common.ast.misc.ConstantNode;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import java.util.Arrays;
import java.util.Map.Entry;
import net.minecraft.util.CubicSpline;
import net.minecraft.util.CubicSpline.Constant;
import net.minecraft.util.CubicSpline.Multipoint;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions.Spline.Coordinate;
import net.minecraft.world.level.levelgen.DensityFunctions.Spline.Point;

public class SplineAstNode implements AstNode {
   public final CubicSpline<Point, Coordinate> spline;
   public final Reference2ReferenceOpenHashMap<Coordinate, AstNode> children = new Reference2ReferenceOpenHashMap();

   public SplineAstNode(CubicSpline<Point, Coordinate> spline) {
      this.spline = spline;
      this.populateChildrenMap(this.spline);
   }

   private SplineAstNode(CubicSpline<Point, Coordinate> spline, Reference2ReferenceMap<Coordinate, AstNode> children) {
      this.spline = spline;
      this.children.putAll(children);
   }

   @Override
   public AstNode[] getChildren() {
      return (AstNode[])this.children.values().toArray(AstNode[]::new);
   }

   @Override
   public AstNode transform(AstTransformer transformer) {
      boolean isModified = false;
      Reference2ReferenceMap<Coordinate, AstNode> modified = new Reference2ReferenceOpenHashMap(this.children);
      ObjectIterator var4 = modified.entrySet().iterator();

      while (var4.hasNext()) {
         Entry<Coordinate, AstNode> entry = (Entry<Coordinate, AstNode>)var4.next();
         AstNode node = entry.getValue();
         AstNode transformed = node.transform(transformer);
         if (node != transformed) {
            isModified = true;
            entry.setValue(transformed);
         }
      }

      return isModified ? transformer.transform(new SplineAstNode(this.spline, modified)) : transformer.transform(this);
   }

   public static AstNode needOptimizeSameLocationFunction(SplineAstNode node, CubicSpline<Point, Coordinate>... splines) {
      if (splines.length == 0) {
         return null;
      } else if (splines[0] instanceof Multipoint<Point, Coordinate> spline0) {
         AstNode a1Ast = (AstNode)node.children.get(spline0.coordinate());
         if (a1Ast instanceof ConstantNode) {
            return null;
         } else {
            int i = 1;

            for (int splinesLength = splines.length; i < splinesLength; i++) {
               if (!(splines[i] instanceof Multipoint<Point, Coordinate> b1)) {
                  return null;
               }

               AstNode b1Ast = (AstNode)node.children.get(b1.coordinate());
               if (!a1Ast.equals(b1Ast)) {
                  return null;
               }
            }

            return a1Ast;
         }
      } else {
         return null;
      }
   }

   private void populateChildrenMap(CubicSpline<Point, Coordinate> a) {
      if (a instanceof Multipoint<Point, Coordinate> a1) {
         for (CubicSpline<Point, Coordinate> spline : a1.values()) {
            this.populateChildrenMap(spline);
         }

         Coordinate locationFunction = (Coordinate)a1.coordinate();
         this.children.put(locationFunction, McToAst.toAst((DensityFunction)locationFunction.function().value()));
      }
   }

   private static boolean deepEquals(
      CubicSpline<Point, Coordinate> a,
      Reference2ReferenceMap<Coordinate, AstNode> childrenA,
      CubicSpline<Point, Coordinate> b,
      Reference2ReferenceMap<Coordinate, AstNode> childrenB
   ) {
      if (a instanceof Constant<Point, Coordinate> a1 && b instanceof Constant<Point, Coordinate> b1) {
         return a1.value() == b1.value();
      }

      if (a instanceof Multipoint<Point, Coordinate> a1 && b instanceof Multipoint<Point, Coordinate> b1) {
         boolean equals1 = Arrays.equals(a1.derivatives(), b1.derivatives())
            && Arrays.equals(a1.locations(), b1.locations())
            && a1.values().size() == b1.values().size()
            && ((AstNode)childrenA.get(a1.coordinate())).equals(childrenB.get(b1.coordinate()));
         if (!equals1) {
            return false;
         }

         int size = a1.values().size();

         for (int i = 0; i < size; i++) {
            if (!deepEquals((CubicSpline<Point, Coordinate>)a1.values().get(i), childrenA, (CubicSpline<Point, Coordinate>)b1.values().get(i), childrenB)) {
               return false;
            }
         }

         return true;
      }

      return false;
   }

   private static boolean deepRelaxedEquals(
      CubicSpline<Point, Coordinate> a,
      Reference2ReferenceMap<Coordinate, AstNode> childrenA,
      CubicSpline<Point, Coordinate> b,
      Reference2ReferenceMap<Coordinate, AstNode> childrenB
   ) {
      if (a instanceof Constant<Point, Coordinate> a1 && b instanceof Constant<Point, Coordinate> b1) {
         return a1.value() == b1.value();
      }

      if (a instanceof Multipoint<Point, Coordinate> a1 && b instanceof Multipoint<Point, Coordinate> b1) {
         boolean equals1 = Arrays.equals(a1.derivatives(), b1.derivatives())
            && Arrays.equals(a1.locations(), b1.locations())
            && a1.values().size() == b1.values().size()
            && ((AstNode)childrenA.get(a1.coordinate())).relaxedEquals((AstNode)childrenB.get(b1.coordinate()));
         if (!equals1) {
            return false;
         }

         int size = a1.values().size();

         for (int i = 0; i < size; i++) {
            if (!deepRelaxedEquals((CubicSpline<Point, Coordinate>)a1.values().get(i), childrenA, (CubicSpline<Point, Coordinate>)b1.values().get(i), childrenB)
               )
             {
               return false;
            }
         }

         return true;
      }

      return false;
   }

   private static int deepHashcode(CubicSpline<Point, Coordinate> a, Reference2ReferenceMap<Coordinate, AstNode> childrenA) {
      if (a instanceof Constant<Point, Coordinate> a1) {
         return Float.hashCode(a1.value());
      } else if (!(a instanceof Multipoint<Point, Coordinate> a1)) {
         return a.hashCode();
      } else {
         int result = 1;
         result = 31 * result + Arrays.hashCode(a1.derivatives());
         result = 31 * result + Arrays.hashCode(a1.locations());

         for (CubicSpline<Point, Coordinate> spline : a1.values()) {
            result = 31 * result + deepHashcode(spline, childrenA);
         }

         return 31 * result + ((AstNode)childrenA.get(a1.coordinate())).hashCode();
      }
   }

   private static int deepRelaxedHashcode(CubicSpline<Point, Coordinate> a, Reference2ReferenceMap<Coordinate, AstNode> childrenA) {
      if (a instanceof Constant<Point, Coordinate> a1) {
         return Float.hashCode(a1.value());
      } else if (!(a instanceof Multipoint<Point, Coordinate> a1)) {
         return a.hashCode();
      } else {
         int result = 1;
         result = 31 * result + Arrays.hashCode(a1.derivatives());
         result = 31 * result + Arrays.hashCode(a1.locations());

         for (CubicSpline<Point, Coordinate> spline : a1.values()) {
            result = 31 * result + deepRelaxedHashcode(spline, childrenA);
         }

         return 31 * result + ((AstNode)childrenA.get(a1.coordinate())).relaxedHashCode();
      }
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         SplineAstNode that = (SplineAstNode)o;
         return deepEquals(this.spline, this.children, that.spline, that.children);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return deepHashcode(this.spline, this.children);
   }

   @Override
   public boolean relaxedEquals(AstNode o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         SplineAstNode that = (SplineAstNode)o;
         return deepRelaxedEquals(this.spline, this.children, that.spline, that.children);
      } else {
         return false;
      }
   }

   @Override
   public int relaxedHashCode() {
      return deepRelaxedHashcode(this.spline, this.children);
   }
}
