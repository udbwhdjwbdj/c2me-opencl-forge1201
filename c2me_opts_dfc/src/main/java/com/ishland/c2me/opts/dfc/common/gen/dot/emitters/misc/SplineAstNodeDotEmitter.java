package com.ishland.c2me.opts.dfc.common.gen.dot.emitters.misc;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.ConstantNode;
import com.ishland.c2me.opts.dfc.common.ast.spline.SplineAstNode;
import com.ishland.c2me.opts.dfc.common.gen.dot.DotEmitter;
import com.ishland.c2me.opts.dfc.common.gen.dot.DotGen;
import java.util.List;
import net.minecraft.util.CubicSpline;
import net.minecraft.util.CubicSpline.Constant;
import net.minecraft.util.CubicSpline.Multipoint;
import net.minecraft.world.level.levelgen.DensityFunctions.Spline.Coordinate;
import net.minecraft.world.level.levelgen.DensityFunctions.Spline.Point;

public class SplineAstNodeDotEmitter implements DotEmitter<SplineAstNode> {
   public static final SplineAstNodeDotEmitter INSTANCE = new SplineAstNodeDotEmitter();

   private SplineAstNodeDotEmitter() {
   }

   public int doDotGen(SplineAstNode node, DotGen.Context context, DotGen.Context.Builder builder) {
      return builder.ovalShape().label("Spline entry").edge(doDotGenSpline(node, context, node.spline)).label("spline").finish().build();
   }

   private static int doDotGenSpline(SplineAstNode node, DotGen.Context context, CubicSpline<Point, Coordinate> spline) {
      if (spline instanceof Constant<Point, Coordinate> a1) {
         return context.generate(new ConstantNode((double)a1.value()));
      } else if (spline instanceof Multipoint<Point, Coordinate> a1) {
         DotGen.Context.Builder builder = context.getSplineBuilder(spline);
         if (builder.isFrozen()) {
            return builder.getId();
         } else {
            builder.hexagonShape().label("Spline").edge(context.generate((AstNode)node.children.get(a1.coordinate()))).label("locationFunction").finish();
            DotGen.Context.Builder tableBuilder = context.createExtraBuilder();
            StringBuilder table = new StringBuilder();
            table.append('<');
            table.append("<TABLE>");
            table.append("<TR><TD>idx</TD><TD>derivatives</TD><TD>locations</TD><TD>values</TD></TR>");
            List<CubicSpline<Point, Coordinate>> values = a1.values();
            int i = 0;

            for (int valuesSize = values.size(); i < valuesSize; i++) {
               CubicSpline<Point, Coordinate> child = values.get(i);
               table.append("<TR>")
                  .append("<TD>")
                  .append(i)
                  .append("</TD>")
                  .append("<TD>")
                  .append(a1.derivatives()[i])
                  .append("</TD>")
                  .append("<TD>")
                  .append(a1.locations()[i])
                  .append("</TD>");
               if (child instanceof Constant<Point, Coordinate> fixedFloatFunction) {
                  table.append("<TD>").append(fixedFloatFunction.value()).append("</TD>");
               } else {
                  int childId = doDotGenSpline(node, context, child);
                  tableBuilder.edge(childId).label(String.format("children[%d]", i)).finish();
                  table.append("<TD>").append("children.id=").append(DotGen.Context.base26(childId)).append("</TD>");
               }

               table.append("</TR>");
            }

            table.append("</TABLE>");
            table.append(">");
            i = tableBuilder.boxShape().label(table.toString()).build();
            builder.edge(i).label("SplineTable").finish();
            return builder.build();
         }
      } else {
         throw new UnsupportedOperationException(String.format("Unsupported spline implementation: %s", spline.getClass().getName()));
      }
   }
}
