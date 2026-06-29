package com.wilkcraft.raftworld.world;

import com.wilkcraft.raftworld.command.RaftCommand;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.minecraft.core.BlockPos;

import java.util.Random;

@EventBusSubscriber
public class WaterItemSpawner {

  private static final Random RANDOM = new Random();

  @SubscribeEvent
  public static void onWorldTick(LevelTickEvent.Post event) {

    if (!(event.getLevel() instanceof ServerLevel level))
      return;

    if (!level.dimension().equals(RaftCommand.RAFT_DIM))
      return;

    for (ServerPlayer player : level.players()) {

      double radius = 32;

      for (ItemEntity item : level.getEntitiesOfClass(
          ItemEntity.class,
          player.getBoundingBox().inflate(radius))) {

        if (!item.isInWater())
          continue;

        if (item.tickCount < 40)
          continue;

        double speedX = item.getDeltaMovement().x;

        if (speedX > -0.05)
          speedX -= 0.003;

        item.setDeltaMovement(
            Math.max(speedX, -0.05),
            item.getDeltaMovement().y,
            item.getDeltaMovement().z);

        item.hurtMarked = true;
      }
    }

    if (level.getGameTime() % 40 != 0)
      return;

    for (ServerPlayer player : level.players()) {

      BlockPos base = player.blockPosition();

      int x = base.getX() + 18 + RANDOM.nextInt(10);
      int z = base.getZ() + RANDOM.nextInt(20) - 10;
      int y = 90;

      BlockPos spawnPos = new BlockPos(x, y, z);

      if (level.getFluidState(spawnPos).isEmpty())
        continue;

      ItemStack item;

      int roll = RANDOM.nextInt(4);

      if (roll == 0)
        item = new ItemStack(Items.DIRT);
      else if (roll == 1)
        item = new ItemStack(Items.OAK_PLANKS);
      else if (roll == 2)
        item = new ItemStack(Items.STICK);
      else
        item = new ItemStack(Items.ROTTEN_FLESH);

      ItemEntity entity = new ItemEntity(
          level,
          x + 0.5,
          y + 0.5,
          z + 0.5,
          item);

      level.addFreshEntity(entity);
    }
  }
}