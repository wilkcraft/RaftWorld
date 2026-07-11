package com.wilkcraft.raftworld.client;

import com.wilkcraft.raftworld.RaftWorld;
import com.wilkcraft.raftworld.init.ModEntities;
import com.wilkcraft.raftworld.init.ModMenus;
import com.wilkcraft.raftworld.client.screen.RaftNetScreen;
import net.minecraft.client.renderer.entity.DolphinRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Dolphin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = RaftWorld.MODID, value = Dist.CLIENT)
public class ClientEvents {
  @SubscribeEvent
  public static void onRegisterScreens(RegisterMenuScreensEvent event) {
    event.register(ModMenus.RAFT_NET.get(), RaftNetScreen::new);
  }

  @SubscribeEvent
  public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
    event.registerEntityRenderer(ModEntities.RAFT_SHARK.get(), RaftSharkRenderer::new);
  }

  private static class RaftSharkRenderer extends DolphinRenderer {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(RaftWorld.MODID,
        "textures/entity/raft_shark.png");

    RaftSharkRenderer(EntityRendererProvider.Context context) {
      super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(Dolphin entity) {
      return TEXTURE;
    }
  }
}