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

  private static final double DESPAWN_DISTANCE = 100;
  private static final int MAX_LIFETIME = 20 * 60 * 2;
  private static final int MAX_ITEMS_NEAR_PLAYER = 50;

  private static final String FAST_ITEMS_TAG = "raftworld_fast_items";

  @SubscribeEvent
  public static void onWorldTick(LevelTickEvent.Post event) {

    if (!(event.getLevel() instanceof ServerLevel level))
      return;

    if (!level.dimension().equals(RaftCommand.RAFT_DIM))
      return;

    for (ServerPlayer player : level.players()) {

      double radius = DESPAWN_DISTANCE;

      for (ItemEntity item : level.getEntitiesOfClass(
          ItemEntity.class,
          player.getBoundingBox().inflate(radius))) {

        if (item.getTags().contains("raftworld_floating")) {

          if (item.tickCount > MAX_LIFETIME) {
            item.discard();
            continue;
          }

          double dx = item.getX() - player.getX();
          double dz = item.getZ() - player.getZ();

          if (dx * dx + dz * dz > DESPAWN_DISTANCE * DESPAWN_DISTANCE) {
            item.discard();
            continue;
          }
        }

        if (!item.isInWater())
          continue;

        if (item.tickCount < 40)
          continue;

        double maxSpeed = -0.05;
        double acceleration = 0.003;

        if (item.getTags().contains("raftworld_fast")) {
          maxSpeed = -0.08;
          acceleration = 0.006;
        }

        double speedX = item.getDeltaMovement().x;

        if (speedX > maxSpeed)
          speedX -= acceleration;

        item.setDeltaMovement(
            Math.max(speedX, maxSpeed),
            item.getDeltaMovement().y,
            item.getDeltaMovement().z);

        item.hurtMarked = true;
      }
    }

    if (level.getGameTime() % 40 != 0)
      return;

    for (ServerPlayer player : level.players()) {

      int nearbyItems = level.getEntitiesOfClass(
          ItemEntity.class,
          player.getBoundingBox().inflate(120)).size();

      if (nearbyItems >= MAX_ITEMS_NEAR_PLAYER)
        continue;

      BlockPos base = player.blockPosition();
      int x = base.getX() + 35 + RANDOM.nextInt(20);
      int z = base.getZ() - 25 + RANDOM.nextInt(51);
      int y = 90;

      BlockPos spawnPos = new BlockPos(x, y, z);

      if (level.getFluidState(spawnPos).isEmpty())
        continue;

      ItemStack item;

      int roll = RANDOM.nextInt(5);

      if (roll == 0)
        item = new ItemStack(Items.DIRT);
      else if (roll == 1)
        item = new ItemStack(Items.OAK_PLANKS);
      else if (roll == 2)
        item = new ItemStack(Items.STICK);
      else if (roll == 3)
        item = new ItemStack(Items.ROTTEN_FLESH);
      else
        item = new ItemStack(Items.OAK_SAPLING);

      ItemEntity entity = new ItemEntity(
          level,
          x + 0.5,
          y + 0.5,
          z + 0.5,
          item);

      entity.addTag("raftworld_floating");

      int remaining = player.getPersistentData().getInt(FAST_ITEMS_TAG);

      if (remaining > 0) {
        entity.addTag("raftworld_fast");

        player.getPersistentData().putInt(
            FAST_ITEMS_TAG,
            remaining - 1);
      }

      level.addFreshEntity(entity);
    }
  }
}