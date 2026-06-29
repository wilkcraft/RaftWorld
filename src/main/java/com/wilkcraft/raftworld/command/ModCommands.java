package com.wilkcraft.raftworld.command;

import com.wilkcraft.raftworld.RaftWorld;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = RaftWorld.MODID)
public class ModCommands {

  @SubscribeEvent
  public static void onRegisterCommands(RegisterCommandsEvent event) {

    RaftCommand.register(event.getDispatcher());

  }

}