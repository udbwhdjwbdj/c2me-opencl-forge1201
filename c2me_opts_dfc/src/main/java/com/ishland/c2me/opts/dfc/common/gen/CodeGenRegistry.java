package com.ishland.c2me.opts.dfc.common.gen;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import java.lang.invoke.VarHandle;

public class CodeGenRegistry<E extends CodeEmitter<? extends AstNode>> {
   private final Reference2ReferenceOpenHashMap<Class<? extends AstNode>, E> exactMatches = new Reference2ReferenceOpenHashMap();
   private volatile boolean frozen = false;

   @SuppressWarnings({"unchecked", "rawtypes"})
   public <N extends AstNode, E1 extends CodeEmitter<N>> void registerExactMatch(Class<N> clazz, E1 emitter) {
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

   public <N extends AstNode, E1 extends CodeEmitter<N>> E1 get(Class<N> clazz) {
      if (!this.frozen) {
         synchronized (this) {
            if (!this.frozen) {
               this.frozen = true;
            }
         }

         VarHandle.fullFence();
      }

      E exactMatch = (E)this.exactMatches.get(clazz);
      if (exactMatch != null) {
         return (E1)exactMatch;
      } else {
         throw new UnsupportedOperationException("Unknown class %s".formatted(clazz));
      }
   }
}
