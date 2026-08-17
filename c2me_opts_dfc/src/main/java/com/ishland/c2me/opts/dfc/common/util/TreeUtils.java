package com.ishland.c2me.opts.dfc.common.util;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MaxShortNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MinShortNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MulNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.EndIslandsNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.RangeChoiceNode;
import com.ishland.c2me.opts.dfc.common.ast.noise.GenericShiftedNoiseNode;
import com.ishland.c2me.opts.dfc.common.ast.spline.SplineNormalNode;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Collection;
import java.util.function.Consumer;

public class TreeUtils {
   private static void enumerate(AstNode node, TreeUtils.AstNodeConsumer consumer) {
      if (consumer.accept(node) == TreeUtils.AstNodeConsumer.IterationBehavior.CONTINUE) {
         for (AstNode child : node.getChildren()) {
            enumerate(child, consumer);
         }
      }
   }

   public static boolean isNonTrivial(AstNode node) {
      return node instanceof GenericShiftedNoiseNode || node instanceof EndIslandsNode;
   }

   public static boolean isBranch(AstNode node) {
      return node instanceof MaxShortNode
         || node instanceof MinShortNode
         || node instanceof MulNode
         || node instanceof RangeChoiceNode
         || node instanceof SplineNormalNode;
   }

   public static boolean hasNonTrivialChildrenUntilBranch(AstNode node) {
      boolean[] result = new boolean[1];
      enumerate(node, node1 -> {
         if (isBranch(node1)) {
            return TreeUtils.AstNodeConsumer.IterationBehavior.STOP_EXPANDING;
         } else {
            if (!result[0] && isNonTrivial(node1)) {
               result[0] = true;
            }

            return result[0] ? TreeUtils.AstNodeConsumer.IterationBehavior.STOP_EXPANDING : TreeUtils.AstNodeConsumer.IterationBehavior.CONTINUE;
         }
      });
      return result[0];
   }

   public static boolean hasNonTrivialChildrenUntilBranchIgnoringMul(AstNode node) {
      boolean[] result = new boolean[1];
      enumerate(node, node1 -> {
         if (isBranch(node1) && !(node1 instanceof MulNode)) {
            return TreeUtils.AstNodeConsumer.IterationBehavior.STOP_EXPANDING;
         } else {
            if (!result[0] && isNonTrivial(node1)) {
               result[0] = true;
            }

            return result[0] ? TreeUtils.AstNodeConsumer.IterationBehavior.STOP_EXPANDING : TreeUtils.AstNodeConsumer.IterationBehavior.CONTINUE;
         }
      });
      return result[0];
   }

   private static void enumerateUntilNonTrivialBranch(AstNode node, Consumer<AstNode> consumer) {
      enumerate(
         node,
         node1 -> {
            consumer.accept(node1);
            return isBranch(node1) && hasNonTrivialChildrenUntilBranch(node1)
               ? TreeUtils.AstNodeConsumer.IterationBehavior.STOP_EXPANDING
               : TreeUtils.AstNodeConsumer.IterationBehavior.CONTINUE;
         }
      );
   }

   public static Collection<AstNode> findLargestCommonSubtrees(AstNode... roots) {
      if (roots.length < 2) {
         throw new IllegalArgumentException("Cannot find largest common subtrees with less than 2 roots");
      } else {
         ObjectLinkedOpenHashSet<AstNode> commonNodes = new ObjectLinkedOpenHashSet();
         enumerateUntilNonTrivialBranch(roots[0], k -> commonNodes.add(k));
         ObjectOpenHashSet<AstNode> tmp = new ObjectOpenHashSet();
         int i = 1;

         for (int rootsLength = roots.length; i < rootsLength; i++) {
            AstNode root = roots[i];
            enumerateUntilNonTrivialBranch(root, tmp::add);
            commonNodes.retainAll(tmp);
            tmp.clear();
         }

         ObjectLinkedOpenHashSet<AstNode> toRemove = new ObjectLinkedOpenHashSet();
         ObjectListIterator var7 = commonNodes.iterator();

         while (var7.hasNext()) {
            AstNode node = (AstNode)var7.next();
            enumerateUntilNonTrivialBranch(node, node1 -> {
               if (node != node1 && commonNodes.contains(node1)) {
                  toRemove.add(node1);
               }
            });
         }

         commonNodes.removeAll(toRemove);
         return commonNodes;
      }
   }

   public interface AstNodeConsumer {
      TreeUtils.AstNodeConsumer.IterationBehavior accept(AstNode var1);

      public static enum IterationBehavior {
         CONTINUE,
         STOP_EXPANDING;
      }
   }
}
