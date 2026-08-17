package com.ishland.c2me.opts.dfc.common.gen.dot.emitters;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.AbsNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.AbstractUnaryNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.CeilNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.CosNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.CubeNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.FloorNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.NegMulNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.SinNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.SqrtNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.SquareNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.SqueezeNode;
import com.ishland.c2me.opts.dfc.common.gen.CodeGenRegistry;
import com.ishland.c2me.opts.dfc.common.gen.dot.DotEmitter;
import com.ishland.c2me.opts.dfc.common.gen.dot.DotGen;
import java.util.Objects;
import java.util.function.Function;

public class UnaryNodeDotEmitters {
   public static void register(CodeGenRegistry<DotEmitter<? extends AstNode>> registry) {
      registry.registerExactMatch(AbsNode.class, new UnaryNodeDotEmitters.UnaryNodeEmitter<>(node -> "abs"));
      registry.registerExactMatch(CeilNode.class, new UnaryNodeDotEmitters.UnaryNodeEmitter<>(node -> "ceil"));
      registry.registerExactMatch(CosNode.class, new UnaryNodeDotEmitters.UnaryNodeEmitter<>(node -> "cos"));
      registry.registerExactMatch(CubeNode.class, new UnaryNodeDotEmitters.UnaryNodeEmitter<>(node -> "cube"));
      registry.registerExactMatch(FloorNode.class, new UnaryNodeDotEmitters.UnaryNodeEmitter<>(node -> "floor"));
      registry.registerExactMatch(NegMulNode.class, new UnaryNodeDotEmitters.UnaryNodeEmitter<>(node -> "NegMul" + node.negMul));
      registry.registerExactMatch(SinNode.class, new UnaryNodeDotEmitters.UnaryNodeEmitter<>(node -> "sin"));
      registry.registerExactMatch(SqrtNode.class, new UnaryNodeDotEmitters.UnaryNodeEmitter<>(node -> "sqrt"));
      registry.registerExactMatch(SquareNode.class, new UnaryNodeDotEmitters.UnaryNodeEmitter<>(node -> "square"));
      registry.registerExactMatch(SqueezeNode.class, new UnaryNodeDotEmitters.UnaryNodeEmitter<>(node -> "squeeze"));
   }

   public static class UnaryNodeEmitter<T extends AbstractUnaryNode> implements DotEmitter<T> {
      private final Function<T, String> description;

      public UnaryNodeEmitter(Function<T, String> description) {
         this.description = Objects.requireNonNull(description);
      }

      public int doDotGen(T node, DotGen.Context context, DotGen.Context.Builder builder) {
         return builder.circleShape().label(this.description.apply(node)).edge(context.generate(node.operand)).label("operand").finish().build();
      }
   }
}
