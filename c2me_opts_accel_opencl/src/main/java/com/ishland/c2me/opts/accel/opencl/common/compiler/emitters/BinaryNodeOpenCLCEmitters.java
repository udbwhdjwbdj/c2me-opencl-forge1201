package com.ishland.c2me.opts.accel.opencl.common.compiler.emitters;

import com.ishland.c2me.opts.accel.opencl.common.Config;
import com.ishland.c2me.opts.accel.opencl.common.compiler.OpenCLCGen;
import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.AbstractBinaryNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.AddNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.DivNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MaxNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MaxShortNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MinNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MinShortNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MulNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.ConstantNode;
import com.ishland.c2me.opts.dfc.common.gen.CodeGenRegistry;
import com.ishland.c2me.opts.dfc.common.gen.meta.ValuesMethodDefF64;
import com.ishland.c2me.opts.dfc.common.gen.opencl.OpenCLCEmitter;
import com.ishland.c2me.opts.dfc.common.gen.opencl.OpenCLCGenFunctionContext;
import com.ishland.c2me.opts.dfc.common.util.TreeUtils;

public class BinaryNodeOpenCLCEmitters {
   public static void register(CodeGenRegistry<OpenCLCEmitter<? extends AstNode>> registry) {
      registry.registerExactMatch(AddNode.class, BinaryNodeOpenCLCEmitters.AddNodeEmitter.INSTANCE);
      registry.registerExactMatch(DivNode.class, BinaryNodeOpenCLCEmitters.DivNodeEmitter.INSTANCE);
      registry.registerExactMatch(MaxNode.class, BinaryNodeOpenCLCEmitters.MaxNodeEmitter.INSTANCE);
      registry.registerExactMatch(MaxShortNode.class, BinaryNodeOpenCLCEmitters.MaxShortNodeEmitter.INSTANCE);
      registry.registerExactMatch(MinNode.class, BinaryNodeOpenCLCEmitters.MinNodeEmitter.INSTANCE);
      registry.registerExactMatch(MinShortNode.class, BinaryNodeOpenCLCEmitters.MinShortNodeEmitter.INSTANCE);
      registry.registerExactMatch(MulNode.class, BinaryNodeOpenCLCEmitters.MulNodeEmitter.INSTANCE);
   }

   public abstract static class AbstractGenericBinaryNodeOpenCLCEmitter<T extends AbstractBinaryNode> implements OpenCLCEmitter<T> {
      public String doCLGen(T node, OpenCLCGenFunctionContext context, String storeTo) {
         StringBuilder sb = new StringBuilder();
         ValuesMethodDefF64 leftMethod = context.newVarF64(node.left);
         ValuesMethodDefF64 rightMethod = context.newVarF64(node.right);
         this.genBody(node, context, storeTo, sb, leftMethod, rightMethod);
         return sb.toString();
      }

      public abstract void genBody(T var1, OpenCLCGenFunctionContext var2, String var3, StringBuilder var4, ValuesMethodDefF64 var5, ValuesMethodDefF64 var6);
   }

   public static class AddNodeEmitter extends BinaryNodeOpenCLCEmitters.AbstractGenericBinaryNodeOpenCLCEmitter<AddNode> {
      public static final BinaryNodeOpenCLCEmitters.AddNodeEmitter INSTANCE = new BinaryNodeOpenCLCEmitters.AddNodeEmitter();

      private AddNodeEmitter() {
      }

      public void genBody(AddNode node, OpenCLCGenFunctionContext context, String storeTo, StringBuilder sb, ValuesMethodDefF64 left, ValuesMethodDefF64 right) {
         sb.append(storeTo).append(" = ").append(context.getDelegateVar(left)).append(" + ").append(context.getDelegateVar(right)).append(";\n");
      }
   }

   public static class DivNodeEmitter extends BinaryNodeOpenCLCEmitters.AbstractGenericBinaryNodeOpenCLCEmitter<DivNode> {
      public static final BinaryNodeOpenCLCEmitters.DivNodeEmitter INSTANCE = new BinaryNodeOpenCLCEmitters.DivNodeEmitter();

      private DivNodeEmitter() {
      }

      public void genBody(DivNode node, OpenCLCGenFunctionContext context, String storeTo, StringBuilder sb, ValuesMethodDefF64 left, ValuesMethodDefF64 right) {
         sb.append(storeTo).append(" = ").append(context.getDelegateVar(left)).append(" / ").append(context.getDelegateVar(right)).append(";\n");
      }
   }

   public static class MaxNodeEmitter extends BinaryNodeOpenCLCEmitters.AbstractGenericBinaryNodeOpenCLCEmitter<MaxNode> {
      public static final BinaryNodeOpenCLCEmitters.MaxNodeEmitter INSTANCE = new BinaryNodeOpenCLCEmitters.MaxNodeEmitter();

      private MaxNodeEmitter() {
      }

      public void genBody(MaxNode node, OpenCLCGenFunctionContext context, String storeTo, StringBuilder sb, ValuesMethodDefF64 left, ValuesMethodDefF64 right) {
         sb.append(storeTo).append(" = fmax(").append(context.getDelegateVar(left)).append(", ").append(context.getDelegateVar(right)).append(");\n");
      }
   }

   public static class MaxShortNodeEmitter extends BinaryNodeOpenCLCEmitters.AbstractGenericBinaryNodeOpenCLCEmitter<MaxShortNode> {
      public static final BinaryNodeOpenCLCEmitters.MaxShortNodeEmitter INSTANCE = new BinaryNodeOpenCLCEmitters.MaxShortNodeEmitter();

      private MaxShortNodeEmitter() {
      }

      public String doCLGen(MaxShortNode node, OpenCLCGenFunctionContext context, String storeTo) {
         StringBuilder sb = new StringBuilder();
         ValuesMethodDefF64 leftMethod = context.newVarF64(node.left);
         sb.append("const double _left = ").append(context.getDelegateVar(leftMethod)).append(";\n");
         sb.append("if (_left >= ").append(OpenCLCGen.literal(node.rightMax)).append(") {\n");
         sb.append("    ").append(storeTo).append(" = _left;\n");
         sb.append("} else {\n");
         if (!Config.preserveAllControlFlows) {
            ValuesMethodDefF64 rightMethod;
            if (TreeUtils.hasNonTrivialChildrenUntilBranch(node.right)) {
               OpenCLCGenFunctionContext forked = context.fork();
               rightMethod = forked.newVarF64(node.right);
               sb.append(forked.getBody().indent(4));
            } else {
               rightMethod = context.newVarF64(node.right);
            }

            sb.append("    ").append(storeTo).append(" = fmax(_left, ").append(context.getDelegateVar(rightMethod)).append(");\n");
         } else {
            ValuesMethodDefF64 rightMethod = context.getGlobalContext().newMethodF64(node.right, context.getVariant());
            sb.append("    ").append(storeTo).append(" = fmax(_left, ").append(context.getGlobalContext().callDelegate(rightMethod)).append(");\n");
         }

         sb.append("}\n");
         return sb.toString();
      }

      public void genBody(
         MaxShortNode node, OpenCLCGenFunctionContext context, String storeTo, StringBuilder sb, ValuesMethodDefF64 left, ValuesMethodDefF64 right
      ) {
         throw new UnsupportedOperationException();
      }
   }

   public static class MinNodeEmitter extends BinaryNodeOpenCLCEmitters.AbstractGenericBinaryNodeOpenCLCEmitter<MinNode> {
      public static final BinaryNodeOpenCLCEmitters.MinNodeEmitter INSTANCE = new BinaryNodeOpenCLCEmitters.MinNodeEmitter();

      private MinNodeEmitter() {
      }

      public void genBody(MinNode node, OpenCLCGenFunctionContext context, String storeTo, StringBuilder sb, ValuesMethodDefF64 left, ValuesMethodDefF64 right) {
         sb.append(storeTo).append(" = fmin(").append(context.getDelegateVar(left)).append(", ").append(context.getDelegateVar(right)).append(");\n");
      }
   }

   public static class MinShortNodeEmitter extends BinaryNodeOpenCLCEmitters.AbstractGenericBinaryNodeOpenCLCEmitter<MinShortNode> {
      public static final BinaryNodeOpenCLCEmitters.MinShortNodeEmitter INSTANCE = new BinaryNodeOpenCLCEmitters.MinShortNodeEmitter();

      private MinShortNodeEmitter() {
      }

      public String doCLGen(MinShortNode node, OpenCLCGenFunctionContext context, String storeTo) {
         StringBuilder sb = new StringBuilder();
         ValuesMethodDefF64 leftMethod = context.newVarF64(node.left);
         sb.append("const double _left = ").append(context.getDelegateVar(leftMethod)).append(";\n");
         sb.append("if (_left <= ").append(OpenCLCGen.literal(node.rightMin)).append(") {\n");
         sb.append("    ").append(storeTo).append(" = _left;\n");
         sb.append("} else {\n");
         if (!Config.preserveAllControlFlows) {
            ValuesMethodDefF64 rightMethod;
            if (TreeUtils.hasNonTrivialChildrenUntilBranch(node.right)) {
               OpenCLCGenFunctionContext forked = context.fork();
               rightMethod = forked.newVarF64(node.right);
               sb.append(forked.getBody().indent(4));
            } else {
               rightMethod = context.newVarF64(node.right);
            }

            sb.append("    ").append(storeTo).append(" = fmin(_left, ").append(context.getDelegateVar(rightMethod)).append(");\n");
         } else {
            ValuesMethodDefF64 rightMethod = context.getGlobalContext().newMethodF64(node.right, context.getVariant());
            sb.append("    ").append(storeTo).append(" = fmin(_left, ").append(context.getGlobalContext().callDelegate(rightMethod)).append(");\n");
         }

         sb.append("}\n");
         return sb.toString();
      }

      public void genBody(
         MinShortNode node, OpenCLCGenFunctionContext context, String storeTo, StringBuilder sb, ValuesMethodDefF64 left, ValuesMethodDefF64 right
      ) {
         throw new UnsupportedOperationException();
      }
   }

   public static class MulNodeEmitter extends BinaryNodeOpenCLCEmitters.AbstractGenericBinaryNodeOpenCLCEmitter<MulNode> {
      public static final BinaryNodeOpenCLCEmitters.MulNodeEmitter INSTANCE = new BinaryNodeOpenCLCEmitters.MulNodeEmitter();

      private MulNodeEmitter() {
      }

      public String doCLGen(MulNode node, OpenCLCGenFunctionContext context, String storeTo) {
         StringBuilder sb = new StringBuilder();
         if (node.left instanceof ConstantNode) {
            ValuesMethodDefF64 leftMethod = context.newVarF64(node.left);
            ValuesMethodDefF64 rightMethod = context.newVarF64(node.right);
            sb.append(storeTo).append(" = ").append(context.getDelegateVar(leftMethod)).append(" * ").append(context.getDelegateVar(rightMethod)).append(";\n");
         } else {
            ValuesMethodDefF64 leftMethod = context.newVarF64(node.left);
            sb.append("const double _left = ").append(context.getDelegateVar(leftMethod)).append(";\n");
            sb.append("if (_left == 0.0) {\n");
            sb.append("    ").append(storeTo).append(" = 0.0;\n");
            sb.append("} else {\n");
            if (!Config.preserveAllControlFlows) {
               ValuesMethodDefF64 rightMethod;
               if (TreeUtils.hasNonTrivialChildrenUntilBranch(node.right)) {
                  OpenCLCGenFunctionContext forked = context.fork();
                  rightMethod = forked.newVarF64(node.right);
                  sb.append(forked.getBody().indent(4));
               } else {
                  rightMethod = context.newVarF64(node.right);
               }

               sb.append("    ").append(storeTo).append(" = _left * ").append(context.getDelegateVar(rightMethod)).append(";\n");
            } else {
               ValuesMethodDefF64 rightMethod = context.getGlobalContext().newMethodF64(node.right, context.getVariant());
               sb.append("    ").append(storeTo).append(" = _left * ").append(context.getGlobalContext().callDelegate(rightMethod)).append(";\n");
            }

            sb.append("}\n");
         }

         return sb.toString();
      }

      public void genBody(MulNode node, OpenCLCGenFunctionContext context, String storeTo, StringBuilder sb, ValuesMethodDefF64 left, ValuesMethodDefF64 right) {
         throw new UnsupportedOperationException();
      }
   }
}
