package com.ishland.c2me.opts.dfc.common.ast.noise;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.AstTransformer;
import java.util.Objects;
import net.minecraft.world.level.levelgen.DensityFunction.NoiseHolder;
import net.minecraft.world.level.levelgen.DensityFunctions.WeirdScaledSampler.RarityValueMapper;

public class DFTWeirdScaledSamplerNode implements AstNode {
   public final AstNode input;
   public final NoiseHolder noise;
   public final RarityValueMapper mapper;

   public DFTWeirdScaledSamplerNode(AstNode input, NoiseHolder noise, RarityValueMapper mapper) {
      this.input = Objects.requireNonNull(input);
      this.noise = Objects.requireNonNull(noise);
      this.mapper = Objects.requireNonNull(mapper);
   }

   @Override
   public AstNode[] getChildren() {
      return new AstNode[]{this.input};
   }

   @Override
   public AstNode transform(AstTransformer transformer) {
      AstNode input = this.input.transform(transformer);
      return input == this.input ? transformer.transform(this) : transformer.transform(new DFTWeirdScaledSamplerNode(input, this.noise, this.mapper));
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         DFTWeirdScaledSamplerNode that = (DFTWeirdScaledSamplerNode)o;
         return Objects.equals(this.input, that.input) && Objects.equals(this.noise, that.noise) && this.mapper == that.mapper;
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      int result = 1;
      result = 31 * result + this.getClass().hashCode();
      result = 31 * result + this.input.hashCode();
      result = 31 * result + this.noise.hashCode();
      return 31 * result + this.mapper.hashCode();
   }

   @Override
   public boolean relaxedEquals(AstNode o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         DFTWeirdScaledSamplerNode that = (DFTWeirdScaledSamplerNode)o;
         return this.input.relaxedEquals(that.input) && Objects.equals(this.noise, that.noise) && this.mapper == that.mapper;
      } else {
         return false;
      }
   }

   @Override
   public int relaxedHashCode() {
      int result = 1;
      result = 31 * result + this.getClass().hashCode();
      result = 31 * result + this.input.relaxedHashCode();
      result = 31 * result + this.noise.hashCode();
      return 31 * result + this.mapper.hashCode();
   }
}
