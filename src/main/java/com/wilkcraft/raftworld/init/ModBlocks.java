package com.wilkcraft.raftworld.init;

import com.wilkcraft.raftworld.RaftWorld;
import com.wilkcraft.raftworld.block.RaftNetBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
        public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RaftWorld.MODID);
        public static final DeferredBlock<Block> RAFT_NET = BLOCKS.register("raft_net",
                        () -> new RaftNetBlock(
                                        BlockBehaviour.Properties.of()
                                                        .strength(2.0F)
                                                        .destroyTime(2.0F)
                                                        .noOcclusion()
                                                        .lightLevel(state -> 0)
                                                        .sound(SoundType.WOOD)));
}