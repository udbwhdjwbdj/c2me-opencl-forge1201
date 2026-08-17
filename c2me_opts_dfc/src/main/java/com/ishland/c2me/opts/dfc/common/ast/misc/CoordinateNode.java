package com.ishland.c2me.opts.dfc.common.ast.misc;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.AstTransformer;
import java.util.Objects;

public class CoordinateNode implements AstNode {
   public static final CoordinateNode AXIS_X = new CoordinateNode(CoordinateNode.Axis.X);
   public static final CoordinateNode AXIS_Y = new CoordinateNode(CoordinateNode.Axis.Y);
   public static final CoordinateNode AXIS_Z = new CoordinateNode(CoordinateNode.Axis.Z);
   public final CoordinateNode.Axis axis;

   public CoordinateNode(CoordinateNode.Axis axis) {
      this.axis = Objects.requireNonNull(axis);
   }

   @Override
   public AstNode[] getChildren() {
      return new AstNode[0];
   }

   @Override
   public AstNode transform(AstTransformer transformer) {
      return transformer.transform(this);
   }

   @Override
   public boolean equals(Object o) {
      if (o != null && this.getClass() == o.getClass()) {
         CoordinateNode that = (CoordinateNode)o;
         return this.axis == that.axis;
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return this.axis.hashCode();
   }

   @Override
   public boolean relaxedEquals(AstNode o) {
      return this.equals(o);
   }

   @Override
   public int relaxedHashCode() {
      return this.hashCode();
   }

   public static enum Axis {
      X,
      Y,
      Z;
   }
}
