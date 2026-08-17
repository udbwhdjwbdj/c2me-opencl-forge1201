package com.ishland.c2me.opts.accel.opencl.common.compiler.emitters.misc;

import com.ishland.c2me.opts.accel.opencl.common.Config;
import com.ishland.c2me.opts.accel.opencl.common.compiler.OpenCLCGen;
import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.RangeChoiceNode;
import com.ishland.c2me.opts.dfc.common.gen.meta.ValuesMethodDefF64;
import com.ishland.c2me.opts.dfc.common.gen.opencl.OpenCLCEmitter;
import com.ishland.c2me.opts.dfc.common.gen.opencl.OpenCLCGenFunctionContext;
import com.ishland.c2me.opts.dfc.common.util.TreeUtils;

public class RangeChoiceNodeOpenCLCEmitter implements OpenCLCEmitter<RangeChoiceNode> {
   public static final RangeChoiceNodeOpenCLCEmitter INSTANCE = new RangeChoiceNodeOpenCLCEmitter();

   private RangeChoiceNodeOpenCLCEmitter() {
   }

   public String doCLGen(RangeChoiceNode node, OpenCLCGenFunctionContext context, String storeTo) {
      StringBuilder sb = new StringBuilder();
      if (!Config.preserveAllControlFlows) {
         for (AstNode subtree : TreeUtils.findLargestCommonSubtrees(new AstNode[]{node.whenInRange, node.whenOutOfRange})) {
            context.newVar(subtree);
         }
      }

      ValuesMethodDefF64 input = context.newVarF64(node.input);
      sb.append("double v = ").append(context.getDelegateVar(input)).append(";\n");
      sb.append("if (v >= ").append(OpenCLCGen.literal(node.minInclusive)).append(" && v < ").append(OpenCLCGen.literal(node.maxExclusive)).append(") {\n");
      emitCall(context, node.whenInRange, sb, storeTo);
      sb.append("} else {\n");
      emitCall(context, node.whenOutOfRange, sb, storeTo);
      sb.append("}\n");
      return sb.toString();
   }

   private static void emitCall(OpenCLCGenFunctionContext context, AstNode node, StringBuilder sb, String storeTo) {
      if (!Config.preserveAllControlFlows) {
         if (TreeUtils.hasNonTrivialChildrenUntilBranchIgnoringMul(node)) {
            OpenCLCGenFunctionContext forked = context.fork();
            ValuesMethodDefF64 whenInRange = forked.newVarF64(node);
            sb.append(forked.getBody().indent(4));
            sb.append("    ").append(storeTo).append(" = ").append(forked.getDelegateVar(whenInRange)).append(";\n");
         } else {
            ValuesMethodDefF64 whenInRange = context.newVarF64(node);
            sb.append("    ").append(storeTo).append(" = ").append(context.getDelegateVar(whenInRange)).append(";\n");
         }
      } else {
         ValuesMethodDefF64 whenInRange = context.getGlobalContext().newMethodF64(node, context.getVariant());
         sb.append("    ").append(storeTo).append(" = ").append(context.getGlobalContext().callDelegate(whenInRange)).append(";\n");
      }
   }
}
