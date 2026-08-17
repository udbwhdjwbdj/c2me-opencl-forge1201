package com.ishland.c2me.opts.dfc.common.ast;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import java.lang.invoke.VarHandle;
import net.minecraft.world.level.levelgen.DensityFunction;

public class FrontendRegistry<E extends AstEmitter<? extends DensityFunction>> {
   private final Reference2ReferenceOpenHashMap<Class<? extends DensityFunction>, E> exactMatches = new Reference2ReferenceOpenHashMap<>();
   private volatile boolean frozen = false;

   @SuppressWarnings("unchecked")
   public <N extends DensityFunction, E1 extends AstEmitter<N>> void registerExactMatch(Class<N> clazz, E1 emitter) {
      if (!this.frozen) {
         synchronized (this) {
            if (!this.frozen) {
               VarHandle.fullFence();
               if (this.exactMatches.containsKey(clazz)) {
                  throw new IllegalArgumentException("Already registered");
               }
               this.exactMatches.put(clazz, (E) emitter);
               VarHandle.fullFence();
               return;
            }
         }
      }
      throw new IllegalStateException("Already frozen");
   }

   @SuppressWarnings("unchecked")
   public <N extends DensityFunction, E1 extends AstEmitter<N>> E1 getOptional(Class<N> clazz) {
      if (!this.frozen) {
         synchronized (this) {
            if (!this.frozen) {
               this.frozen = true;
            }
         }
         VarHandle.fullFence();
      }
      E exactMatch = this.exactMatches.get(clazz);
      return (E1) (exactMatch != null ? exactMatch : null);
   }
}
