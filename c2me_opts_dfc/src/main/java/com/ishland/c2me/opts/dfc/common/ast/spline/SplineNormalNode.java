package com.ishland.c2me.opts.dfc.common.ast.spline;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.AstTransformer;
import java.util.Arrays;
import java.util.Objects;

public class SplineNormalNode implements AstNode {
   public final AstNode locationFunction;
   public final float[] locations;
   public final AstNode[] values;
   public final float[] derivatives;

   public SplineNormalNode(AstNode locationFunction, float[] locations, AstNode[] values, float[] derivatives) {
      this.locationFunction = Objects.requireNonNull(locationFunction);
      this.locations = Objects.requireNonNull(locations);
      this.values = Objects.requireNonNull(values);
      this.derivatives = Objects.requireNonNull(derivatives);
   }

   @Override
   public AstNode[] getChildren() {
      AstNode[] nodes = new AstNode[this.values.length + 1];
      nodes[0] = this.locationFunction;
      System.arraycopy(this.values, 0, nodes, 1, this.values.length);
      return nodes;
   }

   @Override
   public AstNode transform(AstTransformer transformer) {
      boolean changed = false;
      AstNode transformedLocationFunction = this.locationFunction.transform(transformer);
      if (transformedLocationFunction != this.locationFunction) {
         changed |= true;
      }

      AstNode[] transformedValues = (AstNode[])this.values.clone();
      int i = 0;

      for (int transformedValuesLength = transformedValues.length; i < transformedValuesLength; i++) {
         AstNode transformedFunction = transformedValues[i];
         transformedValues[i] = transformedFunction.transform(transformer);
         if (transformedValues[i] != transformedFunction) {
            changed |= true;
         }
      }

      return !changed
         ? transformer.transform(this)
         : transformer.transform(
            new SplineNormalNode(transformedLocationFunction, (float[])this.locations.clone(), transformedValues, (float[])this.derivatives.clone())
         );
   }

   @Override
   public boolean relaxedEquals(AstNode o) {
      if (o != null && this.getClass() == o.getClass()) {
         SplineNormalNode that = (SplineNormalNode)o;
         if (!this.locationFunction.relaxedEquals(that.locationFunction)) {
            return false;
         } else if (!Arrays.equals(this.locations, that.locations)) {
            return false;
         } else {
            int length = this.values.length;
            if (that.values.length != length) {
               return false;
            } else {
               for (int i = 0; i < length; i++) {
                  AstNode e1 = this.values[i];
                  AstNode e2 = that.values[i];
                  if (!e1.relaxedEquals(e2)) {
                     return false;
                  }
               }

               return Arrays.equals(this.derivatives, that.derivatives);
            }
         }
      } else {
         return false;
      }
   }

   @Override
   public int relaxedHashCode() {
      int result = 1;
      result = 31 * result + this.locationFunction.relaxedHashCode();
      result = 31 * result + Arrays.hashCode(this.locations);

      for (AstNode value : this.values) {
         result = 31 * result + value.relaxedHashCode();
      }

      return 31 * result + Arrays.hashCode(this.derivatives);
   }

   @Override
   public boolean equals(Object o) {
      if (o != null && this.getClass() == o.getClass()) {
         SplineNormalNode that = (SplineNormalNode)o;
         if (!this.locationFunction.equals(that.locationFunction)) {
            return false;
         } else if (!Arrays.equals(this.locations, that.locations)) {
            return false;
         } else {
            int length = this.values.length;
            if (that.values.length != length) {
               return false;
            } else {
               for (int i = 0; i < length; i++) {
                  AstNode e1 = this.values[i];
                  AstNode e2 = that.values[i];
                  if (!e1.equals(e2)) {
                     return false;
                  }
               }

               return Arrays.equals(this.derivatives, that.derivatives);
            }
         }
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      int result = 1;
      result = 31 * result + this.locationFunction.hashCode();
      result = 31 * result + Arrays.hashCode(this.locations);

      for (AstNode value : this.values) {
         result = 31 * result + value.hashCode();
      }

      return 31 * result + Arrays.hashCode(this.derivatives);
   }

   @Override
   public AstNode.ReturnType getReturnType() {
      return AstNode.ReturnType.F32;
   }
}
