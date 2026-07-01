package com.wilkcraft.raftworld.init;

import com.wilkcraft.raftworld.RaftWorld;

import net.minecraft.world.item.CreativeModeTabs;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@EventBusSubscriber(modid = RaftWorld.MODID)
public class ModCreativeTabs {

  @SubscribeEvent
  public static void buildContents(BuildCreativeModeTabContentsEvent event) {

    if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
      event.accept(ModItems.RAFT_NET);
    }

  }

}