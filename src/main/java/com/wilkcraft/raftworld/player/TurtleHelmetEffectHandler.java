package com.wilkcraft.raftworld.player;

import com.wilkcraft.raftworld.RaftWorld;
import com.wilkcraft.raftworld.init.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = RaftWorld.MODID)
public class TurtleHelmetEffectHandler {
  private static final String CHARGE_TAG = "raftworld_wb_charge";
  private static final String SURFACE_TIMER_TAG = "raftworld_wb_surface_timer";
  private static final String INIT_TAG = "raftworld_wb_initialized";

  private static final int MAX_CHARGE_SECONDS = 240;
  private static final int COOLDOWN_SECONDS = 60;

  @SubscribeEvent
  public static void onPlayerTick(PlayerTickEvent.Post event) {
    if (!(event.getEntity() instanceof ServerPlayer player))
      return;

    ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
    if (!head.is(ModItems.ENCHANTED_TURTLE_HELMET.get()))
      return;

    if (player.tickCount % 20 != 0)
      return;

    var data = player.getPersistentData();
    if (!data.getBoolean(INIT_TAG)) {
      data.putBoolean(INIT_TAG, true);
      data.putInt(CHARGE_TAG, MAX_CHARGE_SECONDS);
      data.putInt(SURFACE_TIMER_TAG, COOLDOWN_SECONDS);
    }

    int charge = data.getInt(CHARGE_TAG);
    int surfaceTimer = data.getInt(SURFACE_TIMER_TAG);

    if (player.isUnderWater()) {
      surfaceTimer = 0;
      if (charge > 0) {
        player.addEffect(new MobEffectInstance(
            MobEffects.WATER_BREATHING,
            charge * 20 + 20,
            0, false, false, true));
        charge--;
      }
    } else {
      if (charge < MAX_CHARGE_SECONDS) {
        if (surfaceTimer < COOLDOWN_SECONDS) {
          surfaceTimer++;
        } else {
          charge = Math.min(MAX_CHARGE_SECONDS, charge + 1);
        }
      }
    }

    data.putInt(CHARGE_TAG, charge);
    data.putInt(SURFACE_TIMER_TAG, surfaceTimer);
  }
}