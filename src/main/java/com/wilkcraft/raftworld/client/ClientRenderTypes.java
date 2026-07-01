package com.wilkcraft.raftworld.client;

import com.wilkcraft.raftworld.RaftWorld;
import com.wilkcraft.raftworld.init.ModBlocks;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = RaftWorld.MODID, value = Dist.CLIENT)
public class ClientRenderTypes {
  @SuppressWarnings("deprecation")
  @SubscribeEvent
  public static void onClientSetup(FMLClientSetupEvent event) {
    event.enqueueWork(() -> {
      ItemBlockRenderTypes.setRenderLayer(ModBlocks.RAFT_NET.get(), RenderType.cutout());
    });
  }
}