package com.ishland.c2me.opts.accel.opencl.common.compiler.emitters.misc;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class CLBlockStateMappings {
   private static final BlockState[] DEFAULT_BLOCK_STATE_MAPPINGS = new BlockState[]{
      null,
      Blocks.AIR.defaultBlockState(),
      null,
      Blocks.WATER.defaultBlockState(),
      Blocks.LAVA.defaultBlockState(),
      Blocks.COPPER_ORE.defaultBlockState(),
      Blocks.RAW_COPPER_BLOCK.defaultBlockState(),
      Blocks.GRANITE.defaultBlockState(),
      Blocks.DEEPSLATE_IRON_ORE.defaultBlockState(),
      Blocks.RAW_IRON_BLOCK.defaultBlockState(),
      Blocks.TUFF.defaultBlockState()
   };
   private final BlockState[] idToBlockState;
   private final Object2IntMap<BlockState> blockStateToId;

   public static CLBlockStateMappings defaultMappings(BlockState defaultBlock, BlockState defaultFluid) {
      ArrayList<BlockState> states = new ArrayList<>(Arrays.asList((BlockState[])DEFAULT_BLOCK_STATE_MAPPINGS.clone()));
      if (!states.contains(defaultBlock)) {
         states.set(2, defaultBlock);
      }

      if (!states.contains(defaultFluid)) {
         states.add(defaultFluid);
      }

      return new CLBlockStateMappings(states.toArray(BlockState[]::new));
   }

   public CLBlockStateMappings(BlockState[] idToBlockState) {
      this.idToBlockState = idToBlockState;
      Object2IntMap<BlockState> blockStateToId = new Object2IntOpenHashMap();
      BlockState[] toBlockState = this.idToBlockState;
      int i = 0;

      for (int toBlockStateLength = toBlockState.length; i < toBlockStateLength; i++) {
         BlockState blockState = toBlockState[i];
         blockStateToId.put(blockState, i);
      }

      blockStateToId.defaultReturnValue(Integer.MAX_VALUE);
      this.blockStateToId = blockStateToId;
   }

   public int toId(BlockState blockState) {
      int id = this.blockStateToId.getInt(blockState);
      if (id == Integer.MAX_VALUE) {
         throw new IllegalArgumentException("BlockState not found in mapping: " + blockState);
      } else {
         return id;
      }
   }

   public BlockState[] getIdToBlockState() {
      return this.idToBlockState;
   }

   public Object2IntMap<BlockState> getBlockStateToId() {
      return this.blockStateToId;
   }

   public BlockState getBlockState(int value) {
      return this.idToBlockState[value];
   }
}
