package com.wilkcraft.raftworld;

import com.wilkcraft.raftworld.entity.RaftSharkEntity;
import com.wilkcraft.raftworld.init.ModBlockEntities;
import com.wilkcraft.raftworld.init.ModBlocks;
import com.wilkcraft.raftworld.init.ModEntities;
import com.wilkcraft.raftworld.init.ModItems;
import com.wilkcraft.raftworld.init.ModMenus;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

@Mod(RaftWorld.MODID)
public class RaftWorld {
  public static final String MODID = "raftworld";

  public RaftWorld(IEventBus modBus) {
    ModBlocks.BLOCKS.register(modBus);
    ModItems.ITEMS.register(modBus);
    ModBlockEntities.BLOCK_ENTITIES.register(modBus);
    ModMenus.MENUS.register(modBus);
    ModEntities.ENTITY_TYPES.register(modBus);
    modBus.addListener(this::registerCapabilities);
    modBus.addListener(this::registerAttributes);
  }

  private void registerCapabilities(RegisterCapabilitiesEvent event) {
    event.registerBlockEntity(
        Capabilities.ItemHandler.BLOCK,
        ModBlockEntities.RAFT_NET.get(),
        (be, side) -> new InvWrapper(be.getInventory()));
  }

  private void registerAttributes(EntityAttributeCreationEvent event) {
    event.put(ModEntities.RAFT_SHARK.get(), RaftSharkEntity.createAttributes().build());
  }
}