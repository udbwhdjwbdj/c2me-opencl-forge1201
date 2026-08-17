package com.ishland.c2me.opts.dfc.common.gen.dot.emitters;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.AbstractBinaryNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.AddNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.DivNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MaxNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MaxShortNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MinNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MinShortNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MulNode;
import com.ishland.c2me.opts.dfc.common.gen.CodeGenRegistry;
import com.ishland.c2me.opts.dfc.common.gen.dot.DotEmitter;
import com.ishland.c2me.opts.dfc.common.gen.dot.DotGen;
import java.util.Objects;
import java.util.function.Function;

public class BinaryNodeDotEmitters {
   public static void register(CodeGenRegistry<DotEmitter<? extends AstNode>> registry) {
      registry.registerExactMatch(AddNode.class, new BinaryNodeDotEmitters.BinaryNodeEmitter<>(node -> "add"));
      registry.registerExactMatch(DivNode.class, new BinaryNodeDotEmitters.BinaryNodeEmitter<>(node -> "div"));
      registry.registerExactMatch(MaxNode.class, new BinaryNodeDotEmitters.BinaryNodeEmitter<>(node -> "max"));
      registry.registerExactMatch(MaxShortNode.class, new BinaryNodeDotEmitters.BinaryNodeEmitter<>(node -> "max, shortcut rightMax=" + node.rightMax));
      registry.registerExactMatch(MinNode.class, new BinaryNodeDotEmitters.BinaryNodeEmitter<>(node -> "min"));
      registry.registerExactMatch(MinShortNode.class, new BinaryNodeDotEmitters.BinaryNodeEmitter<>(node -> "min, shortcut rightMin=" + node.rightMin));
      registry.registerExactMatch(MulNode.class, new BinaryNodeDotEmitters.BinaryNodeEmitter<>(node -> "mul"));
   }

   public static class BinaryNodeEmitter<T extends AbstractBinaryNode> implements DotEmitter<T> {
      private final Function<T, String> description;

      public BinaryNodeEmitter(Function<T, String> description) {
         this.description = Objects.requireNonNull(description);
      }

      public int doDotGen(T node, DotGen.Context context, DotGen.Context.Builder builder) {
         return builder.parallelogramShape()
            .label(this.description.apply(node))
            .edge(context.generate(node.left))
            .label("left")
            .finish()
            .edge(context.generate(node.right))
            .label("right")
            .finish()
            .build();
      }
   }
}
