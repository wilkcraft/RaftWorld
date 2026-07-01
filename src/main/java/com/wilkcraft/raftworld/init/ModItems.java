package com.wilkcraft.raftworld.init;

import com.wilkcraft.raftworld.RaftWorld;

import net.minecraft.world.item.BlockItem;

import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {

  public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RaftWorld.MODID);

  public static final DeferredItem<BlockItem> RAFT_NET = ITEMS.registerSimpleBlockItem(
      "raft_net",
      ModBlocks.RAFT_NET);

}