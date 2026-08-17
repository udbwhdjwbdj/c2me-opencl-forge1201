package com.ishland.c2me.opts.dfc.common.gen.dot;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.conversion.ToF32Node;
import com.ishland.c2me.opts.dfc.common.ast.conversion.ToF64Node;
import com.ishland.c2me.opts.dfc.common.ast.integration.lithostitched.misc.MixNode;
import com.ishland.c2me.opts.dfc.common.ast.integration.lithostitched.misc.RepositionNode;
import com.ishland.c2me.opts.dfc.common.ast.integration.lithostitched.misc.SelectNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.BeardifierNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.CacheLikeNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.ConstantF32Node;
import com.ishland.c2me.opts.dfc.common.ast.misc.ConstantNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.CoordinateNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.DelegateNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.EndIslandsNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.FindTopSurfaceNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.InterpolatedNoiseSamplerNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.Multi2SingleNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.RangeChoiceNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.RootNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.YClampedGradientNode;
import com.ishland.c2me.opts.dfc.common.ast.noise.DFTWeirdScaledSamplerNode;
import com.ishland.c2me.opts.dfc.common.ast.noise.GenericShiftedNoiseNode;
import com.ishland.c2me.opts.dfc.common.ast.spline.SplineNormalNode;
import com.ishland.c2me.opts.dfc.common.gen.CodeGenRegistry;
import com.ishland.c2me.opts.dfc.common.gen.dot.emitters.BinaryNodeDotEmitters;
import com.ishland.c2me.opts.dfc.common.gen.dot.emitters.UnaryNodeDotEmitters;
import com.ishland.c2me.opts.dfc.common.gen.dot.emitters.misc.SplineNormalNodeDotEmitter;
import net.minecraft.world.level.levelgen.synth.NormalNoise.NoiseParameters;

public class DotGenRegistry {
   public static final CodeGenRegistry<DotEmitter<? extends AstNode>> REGISTRY = new CodeGenRegistry<>();

   public static <T extends AstNode> int doDotGen(T node, DotGen.Context context, DotGen.Context.Builder builder) {
      @SuppressWarnings({"unchecked", "rawtypes"})
      DotEmitter<T> emitter = (DotEmitter<T>) ((CodeGenRegistry) REGISTRY).get(node.getClass());
      return emitter.doDotGen(node, context, builder);
   }

   static {
      BinaryNodeDotEmitters.register(REGISTRY);
      UnaryNodeDotEmitters.register(REGISTRY);
      REGISTRY.registerExactMatch(
         ToF32Node.class,
         (DotEmitter<ToF32Node>) (node, context, builder) -> builder.triangleShape().label("ToF32").edge(context.generate(node.next)).label("next").finish().build()
      );
      REGISTRY.registerExactMatch(
         ToF64Node.class,
         (DotEmitter<ToF64Node>) (node, context, builder) -> builder.triangleShape().label("ToF64").edge(context.generate(node.next)).label("next").finish().build()
      );
      REGISTRY.registerExactMatch(
         CacheLikeNode.class,
         (DotEmitter<CacheLikeNode>) (node, context, builder) -> builder.folderShape()
               .label("CacheLike\\n" + node.getCacheLike().c2me$describeCacheLike())
               .edge(context.generate(node.getDelegate()))
               .label("delegate")
               .finish()
               .build()
      );
      REGISTRY.registerExactMatch(
         ConstantNode.class,
         (DotEmitter<ConstantNode>) (node, context, builder) -> builder.triangleShape().label(String.valueOf(node.getValue())).build());
      REGISTRY.registerExactMatch(
         ConstantF32Node.class,
         (DotEmitter<ConstantF32Node>) (node, context, builder) -> builder.triangleShape().label(String.valueOf(node.getValue())).build());
      REGISTRY.registerExactMatch(
         CoordinateNode.class,
         (DotEmitter<CoordinateNode>) (node, context, builder) -> builder.triangleShape().label("Coordinate " + node.axis).build());
      REGISTRY.registerExactMatch(
         FindTopSurfaceNode.class,
         (DotEmitter<FindTopSurfaceNode>) (node, context, builder) -> builder.cdsShape()
               .label("FindTopSurface\\ncellHeight=" + node.cellHeight)
               .edge(context.generate(node.upperBound))
               .label("upper bound")
               .finish()
               .edge(context.generate(node.lowerBound))
               .label("lower bound")
               .finish()
               .edge(context.generate(node.density))
               .label("density")
               .finish()
               .build()
      );
      REGISTRY.registerExactMatch(
         GenericShiftedNoiseNode.class,
         (DotEmitter<GenericShiftedNoiseNode>) (node, context, builder) -> builder.hexagonShape()
               .label("GenericShiftedNoise")
               .edge(context.generate(node.inputX))
               .label("inputX")
               .finish()
               .edge(context.generate(node.inputY))
               .label("inputY")
               .finish()
               .edge(context.generate(node.inputZ))
               .label("inputZ")
               .finish()
               .build()
      );
      REGISTRY.registerExactMatch(
         RangeChoiceNode.class,
         (DotEmitter<RangeChoiceNode>) (node, context, builder) -> builder.diamondShape()
               .label("RangeChoice [" + node.minInclusive + ", " + node.maxExclusive + ")")
               .edge(context.generate(node.input))
               .label("input")
               .color("blue")
               .finish()
               .edge(context.generate(node.whenInRange))
               .label("true")
               .finish()
               .edge(context.generate(node.whenOutOfRange))
               .label("false")
               .finish()
               .build()
      );
      REGISTRY.registerExactMatch(
         RootNode.class,
         (DotEmitter<RootNode>) (node, context, builder) -> builder.triangleShape().label("identity").edge(context.generate(node.next)).label("next").finish().build()
      );
      REGISTRY.registerExactMatch(
         YClampedGradientNode.class,
         (DotEmitter<YClampedGradientNode>) (node, context, builder) -> builder.boxShape()
               .label("YClampedGradient\\ny=(" + node.fromY + "," + node.toY + ")\\nvalue=(" + node.fromValue + "," + node.toValue + ")")
               .build()
      );
      REGISTRY.registerExactMatch(
         DFTWeirdScaledSamplerNode.class,
         (DotEmitter<DFTWeirdScaledSamplerNode>) (node, context, builder) -> builder.hexagonShape()
               .label("WeirdScaledSampler\\nmapper=" + node.mapper.getSerializedName() + "\\nnoise=" + node.noise.noiseData().unwrapKey().map(k -> k.location().toString()).orElse("unregistered"))
               .tooltip(((NoiseParameters)node.noise.noiseData().value()).toString())
               .edge(context.generate(node.input))
               .label("input")
               .finish()
               .build()
      );
      REGISTRY.registerExactMatch(
         Multi2SingleNode.class,
         (DotEmitter<Multi2SingleNode>) (node, context, builder) -> builder.triangleShape().label("Multi2Single").edge(context.generate(node.next)).label("next").finish().build()
      );
      REGISTRY.registerExactMatch(SplineNormalNode.class, SplineNormalNodeDotEmitter.INSTANCE);
      REGISTRY.registerExactMatch(
         DelegateNode.class,
         (DotEmitter<DelegateNode>) (node, context, builder) -> builder.trapeziumShape().label(String.format("delegate %s", node.getDelegate())).build()
      );
      REGISTRY.registerExactMatch(
         BeardifierNode.class,
         (DotEmitter<BeardifierNode>) (node, context, builder) -> builder.trapeziumShape().label("Beardifier").build());
      REGISTRY.registerExactMatch(
         EndIslandsNode.class,
         (DotEmitter<EndIslandsNode>) (node, context, builder) -> builder.trapeziumShape().label("EndIslands").build());
      REGISTRY.registerExactMatch(
         InterpolatedNoiseSamplerNode.class,
         (DotEmitter<InterpolatedNoiseSamplerNode>) (node, context, builder) -> {
         StringBuilder sb = new StringBuilder();
         node.sampler.parityConfigString(sb);
         return builder.trapeziumShape().label("InterpolatedNoiseSampler").tooltip(sb.toString()).build();
      });
      REGISTRY.registerExactMatch(
         MixNode.class,
         (DotEmitter<MixNode>) (node, context, builder) -> builder.diamondShape()
               .label("Mix")
               .edge(context.generate(node.input))
               .label("input")
               .color("blue")
               .finish()
               .edge(context.generate(node.argument1))
               .label("left")
               .finish()
               .edge(context.generate(node.argument2))
               .label("right")
               .finish()
               .build()
      );
      REGISTRY.registerExactMatch(
         SelectNode.class,
         (DotEmitter<SelectNode>) (node, context, builder) -> {
            builder.boxShape().label("Select");
            DotGen.Context.Builder tableBuilder = context.createExtraBuilder();
            StringBuilder table = new StringBuilder();
            table.append('<');
            table.append("<TABLE>");
            table.append("<TR><TD>idx</TD><TD>minima</TD><TD>maxima</TD><TD>functions</TD></TR>");
            AstNode[] functions = node.functions;
            int i = 0;

            for (int functionsLength = functions.length; i < functionsLength; i++) {
               table.append("<TR>")
                  .append("<TD>")
                  .append(i)
                  .append("</TD>")
                  .append("<TD>")
                  .append(i < node.mins.length ? node.mins[i] : "")
                  .append("</TD>")
                  .append("<TD>")
                  .append(i < node.maxs.length ? node.maxs[i] : "")
                  .append("</TD>");
               AstNode function = functions[i];
               int childId = context.generate(function);
               tableBuilder.edge(childId).label(String.format("children[%d]", i)).finish();
               table.append("<TD>").append("children.id=").append(DotGen.Context.base26(childId)).append("</TD>");
               table.append("</TR>");
            }

            table.append("</TABLE>");
            table.append(">");
            i = tableBuilder.boxShape().label(table.toString()).build();
            builder.edge(i).label("SelectTable").finish();
            return builder.build();
         }
      );
      REGISTRY.registerExactMatch(
         RepositionNode.class,
         (DotEmitter<RepositionNode>) (node, context, builder) -> builder.hexagonShape()
               .label("Shift")
               .edge(context.generate(node.input))
               .label("input")
               .finish()
               .edge(context.generate(node.inputX))
               .label("inputX")
               .finish()
               .edge(context.generate(node.inputY))
               .label("inputY")
               .finish()
               .edge(context.generate(node.inputZ))
               .label("inputZ")
               .finish()
               .build()
      );
   }
}
