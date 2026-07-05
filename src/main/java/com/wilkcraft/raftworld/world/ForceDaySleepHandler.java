package com.wilkcraft.raftworld.world;

import com.wilkcraft.raftworld.RaftWorld;
import com.wilkcraft.raftworld.command.RaftCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@EventBusSubscriber(modid = RaftWorld.MODID)
public class ForceDaySleepHandler {

  private static final Logger LOGGER = LogUtils.getLogger();

  private static final long DAY_TIME = 1000L;
  private static final long NIGHT_START = 12000L;

  @SubscribeEvent
  public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player))
      return;

    if (!player.level().dimension().equals(RaftCommand.RAFT_DIM))
      return;

    if (player.server == null)
      return;

    ServerLevel overworld = player.server.overworld();

    long dayTime = overworld.getDayTime();
    long timeOfDay = dayTime % 24000L;

    if (timeOfDay < NIGHT_START) {
      return;
    }

    long newDayTime = dayTime - timeOfDay + 24000L + DAY_TIME;
    LOGGER.info("[RaftWorld] Forzando dayTime del Overworld de {} a {}", dayTime, newDayTime);
    overworld.setDayTime(newDayTime);
    overworld.setWeatherParameters(0, 0, false, false);
  }
}