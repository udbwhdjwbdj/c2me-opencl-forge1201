package com.ishland.c2me.opts.dfc.common.ast.integration.lithostitched.misc;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.AstTransformer;
import java.util.Arrays;

public class SelectNode implements AstNode {
   public final AstNode input;
   public final AstNode fallback;
   public final double[] mins;
   public final double[] maxs;
   public final AstNode[] functions;

   public SelectNode(AstNode input, AstNode fallback, double[] mins, double[] maxs, AstNode[] functions) {
      this.input = input;
      this.fallback = fallback;
      this.mins = mins;
      this.maxs = maxs;
      this.functions = functions;
   }

   @Override
   public AstNode[] getChildren() {
      AstNode[] nodes = new AstNode[this.functions.length + 2];
      nodes[0] = this.input;
      nodes[1] = this.fallback;
      System.arraycopy(this.functions, 0, nodes, 2, this.functions.length);
      return nodes;
   }

   @Override
   public AstNode transform(AstTransformer transformer) {
      boolean changed = false;
      AstNode transformedInput = this.input.transform(transformer);
      if (transformedInput != this.input) {
         changed |= true;
      }

      AstNode transformedFallback = this.fallback.transform(transformer);
      if (transformedFallback != this.fallback) {
         changed |= true;
      }

      AstNode[] transformedFunctions = (AstNode[])this.functions.clone();
      int i = 0;

      for (int transformedFunctionsLength = transformedFunctions.length; i < transformedFunctionsLength; i++) {
         AstNode transformedFunction = transformedFunctions[i];
         transformedFunctions[i] = transformedFunction.transform(transformer);
         if (transformedFunctions[i] != transformedFunction) {
            changed |= true;
         }
      }

      return !changed
         ? transformer.transform(this)
         : transformer.transform(
            new SelectNode(transformedInput, transformedFallback, (double[])this.mins.clone(), (double[])this.maxs.clone(), transformedFunctions)
         );
   }

   @Override
   public boolean relaxedEquals(AstNode o) {
      if (o != null && this.getClass() == o.getClass()) {
         SelectNode that = (SelectNode)o;
         if (!this.input.relaxedEquals(that.input) || !Arrays.equals(this.mins, that.mins) || !Arrays.equals(this.maxs, that.maxs)) {
            return false;
         } else if (this.functions == that.functions) {
            return true;
         } else if (this.functions != null && that.functions != null) {
            int length = this.functions.length;
            if (that.functions.length != length) {
               return false;
            } else {
               for (int i = 0; i < length; i++) {
                  AstNode e1 = this.functions[i];
                  AstNode e2 = that.functions[i];
                  if (!e1.relaxedEquals(e2)) {
                     return false;
                  }
               }

               return true;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   @Override
   public int relaxedHashCode() {
      int result = 1;
      result = 31 * result + this.input.relaxedHashCode();
      result = 31 * result + Arrays.hashCode(this.mins);
      result = 31 * result + Arrays.hashCode(this.maxs);

      for (AstNode function : this.functions) {
         result = 31 * result + function.relaxedHashCode();
      }

      return result;
   }

   @Override
   public boolean equals(Object o) {
      if (o != null && this.getClass() == o.getClass()) {
         SelectNode that = (SelectNode)o;
         if (!this.input.equals(that.input) || !Arrays.equals(this.mins, that.mins) || !Arrays.equals(this.maxs, that.maxs)) {
            return false;
         } else if (this.functions == that.functions) {
            return true;
         } else if (this.functions != null && that.functions != null) {
            int length = this.functions.length;
            if (that.functions.length != length) {
               return false;
            } else {
               for (int i = 0; i < length; i++) {
                  AstNode e1 = this.functions[i];
                  AstNode e2 = that.functions[i];
                  if (!e1.equals(e2)) {
                     return false;
                  }
               }

               return true;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      int result = 1;
      result = 31 * result + this.input.hashCode();
      result = 31 * result + Arrays.hashCode(this.mins);
      result = 31 * result + Arrays.hashCode(this.maxs);

      for (AstNode function : this.functions) {
         result = 31 * result + function.hashCode();
      }

      return result;
   }
}
