package com.ishland.c2me.opts.dfc.common.ast.misc;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.AstTransformer;
import java.util.Objects;

public class RangeChoiceNode implements AstNode {
   public final AstNode input;
   public final double minInclusive;
   public final double maxExclusive;
   public final AstNode whenInRange;
   public final AstNode whenOutOfRange;

   public RangeChoiceNode(AstNode input, double minInclusive, double maxExclusive, AstNode whenInRange, AstNode whenOutOfRange) {
      this.input = Objects.requireNonNull(input);
      this.minInclusive = minInclusive;
      this.maxExclusive = maxExclusive;
      this.whenInRange = Objects.requireNonNull(whenInRange);
      this.whenOutOfRange = Objects.requireNonNull(whenOutOfRange);
   }

   @Override
   public AstNode[] getChildren() {
      return new AstNode[]{this.input, this.whenInRange, this.whenOutOfRange};
   }

   @Override
   public AstNode transform(AstTransformer transformer) {
      AstNode input = this.input.transform(transformer);
      AstNode whenInRange = this.whenInRange.transform(transformer);
      AstNode whenOutOfRange = this.whenOutOfRange.transform(transformer);
      return this.input == input && this.whenInRange == whenInRange && this.whenOutOfRange == whenOutOfRange
         ? transformer.transform(this)
         : transformer.transform(new RangeChoiceNode(input, this.minInclusive, this.maxExclusive, whenInRange, whenOutOfRange));
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         RangeChoiceNode that = (RangeChoiceNode)o;
         return Double.compare(this.minInclusive, that.minInclusive) == 0
            && Double.compare(this.maxExclusive, that.maxExclusive) == 0
            && Objects.equals(this.input, that.input)
            && Objects.equals(this.whenInRange, that.whenInRange)
            && Objects.equals(this.whenOutOfRange, that.whenOutOfRange);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      int result = 1;
      result = 31 * result + this.getClass().hashCode();
      result = 31 * result + this.input.hashCode();
      result = 31 * result + Double.hashCode(this.minInclusive);
      result = 31 * result + Double.hashCode(this.maxExclusive);
      result = 31 * result + this.whenInRange.hashCode();
      return 31 * result + this.whenOutOfRange.hashCode();
   }

   @Override
   public boolean relaxedEquals(AstNode o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         RangeChoiceNode that = (RangeChoiceNode)o;
         return Double.compare(this.minInclusive, that.minInclusive) == 0
            && Double.compare(this.maxExclusive, that.maxExclusive) == 0
            && this.input.relaxedEquals(that.input)
            && this.whenInRange.relaxedEquals(that.whenInRange)
            && this.whenOutOfRange.relaxedEquals(that.whenOutOfRange);
      } else {
         return false;
      }
   }

   @Override
   public int relaxedHashCode() {
      int result = 1;
      result = 31 * result + this.getClass().hashCode();
      result = 31 * result + this.input.relaxedHashCode();
      result = 31 * result + Double.hashCode(this.minInclusive);
      result = 31 * result + Double.hashCode(this.maxExclusive);
      result = 31 * result + this.whenInRange.relaxedHashCode();
      return 31 * result + this.whenOutOfRange.relaxedHashCode();
   }
}
