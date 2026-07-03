package com.wilkcraft.raftworld.world;

import com.wilkcraft.raftworld.block.entity.RaftNetBlockEntity;
import com.wilkcraft.raftworld.command.RaftCommand;
import com.wilkcraft.raftworld.init.ModBlocks;
import com.wilkcraft.raftworld.loot.ModLoot;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
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
        if (tryAbsorbIntoRaftNet(level, item)) {
          continue;
        }
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
      ModLoot.checkProgression(player);
      ModLoot.checkDeepDive(player);

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

      int stage = ModLoot.getStage(player);
      ItemStack item = ModLoot.getRandomItem(stage, RANDOM);

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

  private static boolean tryAbsorbIntoRaftNet(ServerLevel level, ItemEntity item) {
    if (item.isRemoved())
      return false;
    ItemStack stack = item.getItem();
    if (stack.isEmpty())
      return false;
    AABB box = item.getBoundingBox().inflate(0.05);
    BlockPos min = BlockPos.containing(box.minX, box.minY, box.minZ);
    BlockPos max = BlockPos.containing(box.maxX, box.maxY, box.maxZ);
    for (int x = min.getX(); x <= max.getX(); x++) {
      for (int y = min.getY(); y <= max.getY(); y++) {
        for (int z = min.getZ(); z <= max.getZ(); z++) {
          BlockPos pos = new BlockPos(x, y, z);
          if (!level.getBlockState(pos).is(ModBlocks.RAFT_NET.get())) {
            continue;
          }
          BlockEntity be = level.getBlockEntity(pos);
          if (!(be instanceof RaftNetBlockEntity netBe)) {
            continue;
          }
          if (insertIntoInventory(netBe.getInventory(), item, stack)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private static boolean insertIntoInventory(SimpleContainer inventory, ItemEntity item, ItemStack stack) {
    for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
      ItemStack slotStack = inventory.getItem(slot);
      if (!slotStack.isEmpty()
          && ItemStack.isSameItemSameComponents(slotStack, stack)
          && slotStack.getCount() < slotStack.getMaxStackSize()) {
        int space = slotStack.getMaxStackSize() - slotStack.getCount();
        int toMove = Math.min(space, stack.getCount());
        slotStack.grow(toMove);
        stack.shrink(toMove);
        inventory.setChanged();
        if (stack.isEmpty()) {
          item.discard();
          return true;
        }
      }
    }
    for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
      if (inventory.getItem(slot).isEmpty()) {
        inventory.setItem(slot, stack.copy());
        item.discard();
        return true;
      }
    }
    return false;
  }
}