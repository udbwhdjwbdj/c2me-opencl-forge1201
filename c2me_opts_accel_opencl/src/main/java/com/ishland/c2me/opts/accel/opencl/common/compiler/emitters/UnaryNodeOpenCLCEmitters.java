package com.ishland.c2me.opts.accel.opencl.common.compiler.emitters;

import com.ishland.c2me.opts.accel.opencl.common.compiler.OpenCLCGen;
import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.AbsNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.AbstractUnaryNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.CubeNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.NegMulNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.SquareNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.SqueezeNode;
import com.ishland.c2me.opts.dfc.common.gen.CodeGenRegistry;
import com.ishland.c2me.opts.dfc.common.gen.meta.ValuesMethodDefF64;
import com.ishland.c2me.opts.dfc.common.gen.opencl.OpenCLCEmitter;
import com.ishland.c2me.opts.dfc.common.gen.opencl.OpenCLCGenFunctionContext;

public class UnaryNodeOpenCLCEmitters {
   public static void register(CodeGenRegistry<OpenCLCEmitter<? extends AstNode>> registry) {
      registry.registerExactMatch(AbsNode.class, UnaryNodeOpenCLCEmitters.AbsNodeEmitter.INSTANCE);
      registry.registerExactMatch(CubeNode.class, UnaryNodeOpenCLCEmitters.CubeNodeEmitter.INSTANCE);
      registry.registerExactMatch(NegMulNode.class, UnaryNodeOpenCLCEmitters.NegMulNodeEmitter.INSTANCE);
      registry.registerExactMatch(SquareNode.class, UnaryNodeOpenCLCEmitters.SquareNodeEmitter.INSTANCE);
      registry.registerExactMatch(SqueezeNode.class, UnaryNodeOpenCLCEmitters.SqueezeNodeEmitter.INSTANCE);
   }

   public static class AbsNodeEmitter extends UnaryNodeOpenCLCEmitters.AbstractGenericUnaryNodeOpenCLCEmitter<AbsNode> {
      public static final UnaryNodeOpenCLCEmitters.AbsNodeEmitter INSTANCE = new UnaryNodeOpenCLCEmitters.AbsNodeEmitter();

      private AbsNodeEmitter() {
      }

      protected void genBody(AbsNode node, OpenCLCGenFunctionContext context, String storeTo, StringBuilder sb, ValuesMethodDefF64 operand) {
         sb.append(storeTo).append(" = fabs(").append(context.getDelegateVar(operand)).append(");\n");
      }
   }

   public abstract static class AbstractGenericUnaryNodeOpenCLCEmitter<T extends AbstractUnaryNode> implements OpenCLCEmitter<T> {
      public String doCLGen(T node, OpenCLCGenFunctionContext context, String storeTo) {
         StringBuilder sb = new StringBuilder();
         ValuesMethodDefF64 operand = context.newVarF64(node.operand);
         this.genBody(node, context, storeTo, sb, operand);
         return sb.toString();
      }

      protected abstract void genBody(T var1, OpenCLCGenFunctionContext var2, String var3, StringBuilder var4, ValuesMethodDefF64 var5);
   }

   public static class CubeNodeEmitter extends UnaryNodeOpenCLCEmitters.AbstractGenericUnaryNodeOpenCLCEmitter<CubeNode> {
      public static final UnaryNodeOpenCLCEmitters.CubeNodeEmitter INSTANCE = new UnaryNodeOpenCLCEmitters.CubeNodeEmitter();

      private CubeNodeEmitter() {
      }

      protected void genBody(CubeNode node, OpenCLCGenFunctionContext context, String storeTo, StringBuilder sb, ValuesMethodDefF64 operand) {
         sb.append("double v = ").append(context.getDelegateVar(operand)).append(";\n").append(storeTo).append(" = v * v * v;\n");
      }
   }

   public static class NegMulNodeEmitter extends UnaryNodeOpenCLCEmitters.AbstractGenericUnaryNodeOpenCLCEmitter<NegMulNode> {
      public static final UnaryNodeOpenCLCEmitters.NegMulNodeEmitter INSTANCE = new UnaryNodeOpenCLCEmitters.NegMulNodeEmitter();

      private NegMulNodeEmitter() {
      }

      protected void genBody(NegMulNode node, OpenCLCGenFunctionContext context, String storeTo, StringBuilder sb, ValuesMethodDefF64 operand) {
         sb.append("double v = ")
            .append(context.getDelegateVar(operand))
            .append(";\n")
            .append(storeTo)
            .append(" = v > 0.0 ? v : v * ")
            .append(OpenCLCGen.literal(node.negMul))
            .append(";\n");
      }
   }

   public static class SquareNodeEmitter extends UnaryNodeOpenCLCEmitters.AbstractGenericUnaryNodeOpenCLCEmitter<SquareNode> {
      public static final UnaryNodeOpenCLCEmitters.SquareNodeEmitter INSTANCE = new UnaryNodeOpenCLCEmitters.SquareNodeEmitter();

      private SquareNodeEmitter() {
      }

      protected void genBody(SquareNode node, OpenCLCGenFunctionContext context, String storeTo, StringBuilder sb, ValuesMethodDefF64 operand) {
         sb.append("double v = ").append(context.getDelegateVar(operand)).append(";\n").append(storeTo).append(" = v * v;\n");
      }
   }

   public static class SqueezeNodeEmitter extends UnaryNodeOpenCLCEmitters.AbstractGenericUnaryNodeOpenCLCEmitter<SqueezeNode> {
      public static final UnaryNodeOpenCLCEmitters.SqueezeNodeEmitter INSTANCE = new UnaryNodeOpenCLCEmitters.SqueezeNodeEmitter();

      private SqueezeNodeEmitter() {
      }

      protected void genBody(SqueezeNode node, OpenCLCGenFunctionContext context, String storeTo, StringBuilder sb, ValuesMethodDefF64 operand) {
         sb.append("double v = clamp(")
            .append(context.getDelegateVar(operand))
            .append(", -1.0, 1.0);\n")
            .append(storeTo)
            .append(" = v / 2.0 - v * v * v / 24.0;\n");
      }
   }
}
