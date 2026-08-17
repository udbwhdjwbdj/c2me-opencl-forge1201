package com.ishland.c2me.opts.dfc.common.gen.dot.emitters.misc;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.ConstantF32Node;
import com.ishland.c2me.opts.dfc.common.ast.spline.SplineNormalNode;
import com.ishland.c2me.opts.dfc.common.gen.dot.DotEmitter;
import com.ishland.c2me.opts.dfc.common.gen.dot.DotGen;

public class SplineNormalNodeDotEmitter implements DotEmitter<SplineNormalNode> {
   public static final SplineNormalNodeDotEmitter INSTANCE = new SplineNormalNodeDotEmitter();

   private SplineNormalNodeDotEmitter() {
   }

   public int doDotGen(SplineNormalNode node, DotGen.Context context, DotGen.Context.Builder builder) {
      builder.hexagonShape().label("SplineNormal").edge(context.generate(node.locationFunction)).label("locationFunction").finish();
      DotGen.Context.Builder tableBuilder = context.createExtraBuilder();
      StringBuilder table = new StringBuilder();
      table.append('<');
      table.append("<TABLE>");
      table.append("<TR><TD>idx</TD><TD>derivatives</TD><TD>locations</TD><TD>values</TD></TR>");
      AstNode[] values = node.values;
      int i = 0;

      for (int valuesSize = values.length; i < valuesSize; i++) {
         AstNode child = values[i];
         table.append("<TR>")
            .append("<TD>")
            .append(i)
            .append("</TD>")
            .append("<TD>")
            .append(node.derivatives[i])
            .append("</TD>")
            .append("<TD>")
            .append(node.locations[i])
            .append("</TD>");
         if (child instanceof ConstantF32Node constantF32Node) {
            table.append("<TD>").append(constantF32Node.getValue()).append("</TD>");
         } else {
            int childId = context.generate(child);
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
}
