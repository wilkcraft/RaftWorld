package com.wilkcraft.raftworld.client;

import com.wilkcraft.raftworld.RaftWorld;
import com.wilkcraft.raftworld.init.ModMenus;
import com.wilkcraft.raftworld.client.screen.RaftNetScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = RaftWorld.MODID, value = Dist.CLIENT)
public class ClientEvents {
  @SubscribeEvent
  public static void onRegisterScreens(RegisterMenuScreensEvent event) {
    event.register(ModMenus.RAFT_NET.get(), RaftNetScreen::new);
  }
}