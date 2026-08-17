package com.ishland.c2me.opts.dfc.common.ast.misc;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import java.util.Objects;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;

public class InterpolatedNoiseSamplerNode extends DelegateNode {
   public final BlendedNoise sampler;

   public InterpolatedNoiseSamplerNode(BlendedNoise sampler) {
      super(sampler);
      this.sampler = sampler;
   }

   @Override
   public boolean equals(Object object) {
      if (this == object) {
         return true;
      } else if (object != null && this.getClass() == object.getClass()) {
         InterpolatedNoiseSamplerNode that = (InterpolatedNoiseSamplerNode)object;
         return Objects.equals(this.sampler, that.sampler);
      } else {
         return false;
      }
   }

   @Override
   public boolean relaxedEquals(AstNode o) {
      return this.equals(o);
   }

   @Override
   public int relaxedHashCode() {
      return this.hashCode();
   }
}
