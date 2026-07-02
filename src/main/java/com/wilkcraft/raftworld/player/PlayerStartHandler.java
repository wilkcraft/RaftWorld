package com.wilkcraft.raftworld.player;

import com.wilkcraft.raftworld.RaftWorld;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = RaftWorld.MODID)
public class PlayerStartHandler {
  private static final String STARTED_TAG = "raftworld_started";

  @SuppressWarnings("deprecation")
  @SubscribeEvent
  public static void onPlayerTick(PlayerTickEvent.Post event) {
    if (!(event.getEntity() instanceof ServerPlayer player))
      return;
    if (!player.level().dimension().equals(Level.OVERWORLD))
      return;
    if (player.getPersistentData().getBoolean(STARTED_TAG))
      return;
    player.fallDistance = 0;
    player.getAbilities().mayfly = false;
    player.getAbilities().flying = false;
    player.getAbilities().setWalkingSpeed(0.0F);
    player.onUpdateAbilities();
    if (player.onGround() && player.getDeltaMovement().y > 0) {
      player.setDeltaMovement(
          0,
          0,
          0);
      player.hurtMarked = true;
    }
    player.addEffect(new MobEffectInstance(
        MobEffects.MOVEMENT_SLOWDOWN,
        5,
        255,
        false,
        false,
        false));
    player.addEffect(new MobEffectInstance(
        MobEffects.BLINDNESS,
        100,
        255,
        false,
        false,
        false));
    player.setDeltaMovement(
        0,
        Math.min(player.getDeltaMovement().y, 0.0D),
        0);
    player.hurtMarked = true;
    if (player.tickCount % 40 == 0) {
      player.connection.send(
          new ClientboundSetTitlesAnimationPacket(
              0,
              50,
              0));
      player.connection.send(
          new ClientboundSetTitleTextPacket(
              Component.translatable("raftworld.hud.title").withStyle(ChatFormatting.GOLD)));
      player.connection.send(
          new ClientboundSetSubtitleTextPacket(
              Component.translatable("raftworld.hud.subtitle").withStyle(ChatFormatting.GRAY)));
    }
  }

  @SubscribeEvent
  public static void onJoin(PlayerEvent.PlayerLoggedInEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player))
      return;
    if (player.level().dimension().equals(Level.OVERWORLD))
      player.getPersistentData().putBoolean("raftworld_started", false);
  }

  @SubscribeEvent
  public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player))
      return;
    if (event.getTo().equals(Level.OVERWORLD))
      player.getPersistentData().putBoolean("raftworld_started", false);
  }
}