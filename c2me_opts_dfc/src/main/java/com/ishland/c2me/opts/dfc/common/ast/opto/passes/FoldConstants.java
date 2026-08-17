package com.ishland.c2me.opts.dfc.common.ast.opto.passes;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.AstTransformer;
import com.ishland.c2me.opts.dfc.common.ast.binary.AddNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MaxNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MaxShortNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MinNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MinShortNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MulNode;
import com.ishland.c2me.opts.dfc.common.ast.integration.lithostitched.misc.MixNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.CacheLikeNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.ConstantNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.AbsNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.CeilNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.CosNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.CubeNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.FloorNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.NegMulNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.SinNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.SqrtNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.SquareNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.SqueezeNode;
import com.ishland.c2me.opts.dfc.common.gen.util.ZeroUtils;
import net.minecraft.util.Mth;

public class FoldConstants implements AstTransformer {
   public static final FoldConstants INSTANCE = new FoldConstants();

   private FoldConstants() {
   }

   @Override
   public AstNode transform(AstNode astNode) {
      if (astNode == null) {
         throw new NullPointerException();
      }

      if (astNode instanceof AddNode addNode) {
         if (addNode.left instanceof ConstantNode c1 && addNode.right instanceof ConstantNode c2) {
            return new ConstantNode(c1.getValue() + c2.getValue());
         }
         if (addNode.left instanceof ConstantNode c && c.getValue() == 0.0 && !ZeroUtils.isPositiveZero(c.getValue())) {
            return addNode.right;
         }
         return addNode;
      }

      if (astNode instanceof MulNode mulNode) {
         if (mulNode.left instanceof ConstantNode c1 && mulNode.right instanceof ConstantNode c2) {
            return new ConstantNode(c1.getValue() * c2.getValue());
         }
         if (mulNode.left instanceof ConstantNode c && c.getValue() == 0.0) {
            return new ConstantNode(0.0);
         }
         if (mulNode.left instanceof ConstantNode c && c.getValue() == 1.0) {
            return mulNode.right;
         }
         return mulNode;
      }

      if (astNode instanceof MaxNode maxNode) {
         if (maxNode.left instanceof ConstantNode c1 && maxNode.right instanceof ConstantNode c2) {
            return new ConstantNode(Math.max(c1.getValue(), c2.getValue()));
         }
         return maxNode;
      }

      if (astNode instanceof MaxShortNode maxShortNode) {
         if (maxShortNode.left instanceof ConstantNode c1 && c1.getValue() >= maxShortNode.rightMax) {
            return c1;
         }
         return !(maxShortNode.left instanceof ConstantNode) && !(maxShortNode.right instanceof ConstantNode)
            ? maxShortNode
            : new MaxNode(maxShortNode.left, maxShortNode.right);
      }

      if (astNode instanceof MinNode minNode) {
         if (minNode.left instanceof ConstantNode c1 && minNode.right instanceof ConstantNode c2) {
            return new ConstantNode(Math.min(c1.getValue(), c2.getValue()));
         }
         return minNode;
      }

      if (astNode instanceof MinShortNode minShortNode) {
         if (minShortNode.left instanceof ConstantNode c1 && c1.getValue() <= minShortNode.rightMin) {
            return c1;
         }
         return !(minShortNode.left instanceof ConstantNode) && !(minShortNode.right instanceof ConstantNode)
            ? minShortNode
            : new MinNode(minShortNode.left, minShortNode.right);
      }

      if (astNode instanceof MixNode mixNode) {
         if (mixNode.input instanceof ConstantNode c1 && mixNode.argument1 instanceof ConstantNode c2 && mixNode.argument2 instanceof ConstantNode c3) {
            return new ConstantNode(c2.getValue() * (1.0 - c1.getValue()) + c3.getValue() * c1.getValue());
         }
         if (mixNode.input instanceof ConstantNode c1 && mixNode.argument1 instanceof ConstantNode c2) {
            return new AddNode(new ConstantNode(c2.getValue() * (1.0 - c1.getValue())), new MulNode(c1, mixNode.argument2));
         }
         if (mixNode.input instanceof ConstantNode c1 && mixNode.argument2 instanceof ConstantNode c2) {
            return new AddNode(new ConstantNode(c2.getValue() * c1.getValue()), new MulNode(new ConstantNode(1.0 - c1.getValue()), mixNode.argument1));
         }
         if (mixNode.input instanceof ConstantNode c1 && c1.getValue() <= 0.0) {
            return mixNode.argument1;
         }
         if (mixNode.input instanceof ConstantNode c1 && c1.getValue() >= 1.0) {
            return mixNode.argument2;
         }
         return mixNode;
      }

      if (astNode instanceof AbsNode absNode) {
         return absNode.operand instanceof ConstantNode c ? new ConstantNode(Math.abs(c.getValue())) : absNode;
      }
      if (astNode instanceof CeilNode ceilNode) {
         return ceilNode.operand instanceof ConstantNode c ? new ConstantNode(Math.ceil(c.getValue())) : ceilNode;
      }
      if (astNode instanceof CosNode cosNode) {
         return cosNode.operand instanceof ConstantNode c ? new ConstantNode(Math.cos(c.getValue())) : cosNode;
      }
      if (astNode instanceof CubeNode cubeNode) {
         return cubeNode.operand instanceof ConstantNode c ? new ConstantNode(c.getValue() * c.getValue() * c.getValue()) : cubeNode;
      }
      if (astNode instanceof FloorNode floorNode) {
         return floorNode.operand instanceof ConstantNode c ? new ConstantNode(Math.floor(c.getValue())) : floorNode;
      }
      if (astNode instanceof NegMulNode negMulNode) {
         return negMulNode.operand instanceof ConstantNode c
            ? new ConstantNode(c.getValue() > 0.0 ? c.getValue() : c.getValue() * negMulNode.negMul)
            : negMulNode;
      }
      if (astNode instanceof SinNode sinNode) {
         return sinNode.operand instanceof ConstantNode c ? new ConstantNode(Math.sin(c.getValue())) : sinNode;
      }
      if (astNode instanceof SqrtNode sqrtNode) {
         return sqrtNode.operand instanceof ConstantNode c ? new ConstantNode(Math.sqrt(c.getValue())) : sqrtNode;
      }
      if (astNode instanceof SquareNode squareNode) {
         return squareNode.operand instanceof ConstantNode c ? new ConstantNode(c.getValue() * c.getValue()) : squareNode;
      }
      if (astNode instanceof SqueezeNode squeezeNode) {
         if (squeezeNode.operand instanceof ConstantNode c) {
            double v = Mth.clamp(c.getValue(), -1.0, 1.0);
            return new ConstantNode(v / 2.0 - v * v * v / 24.0);
         } else {
            return squeezeNode;
         }
      }
      if (astNode instanceof CacheLikeNode cacheLikeNode) {
         if (cacheLikeNode.getCacheLike().c2me$isActualCache() && cacheLikeNode.getDelegate() instanceof ConstantNode c) {
            return c;
         }
         return cacheLikeNode;
      }
      return astNode;
   }
}
