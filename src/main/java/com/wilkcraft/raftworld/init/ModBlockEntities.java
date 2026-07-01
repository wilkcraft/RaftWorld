package com.wilkcraft.raftworld.init;

import com.wilkcraft.raftworld.RaftWorld;
import com.wilkcraft.raftworld.block.entity.RaftNetBlockEntity;

import net.minecraft.world.level.block.entity.BlockEntityType;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister
            .create(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE, RaftWorld.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RaftNetBlockEntity>> RAFT_NET = BLOCK_ENTITIES
            .register("raft_net",
                    () -> BlockEntityType.Builder.of(
                            RaftNetBlockEntity::new,
                            ModBlocks.RAFT_NET.get()).build(null));

}