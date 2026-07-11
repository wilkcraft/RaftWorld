package com.wilkcraft.raftworld.world;

import com.wilkcraft.raftworld.block.entity.RaftNetBlockEntity;
import com.wilkcraft.raftworld.command.RaftCommand;
import com.wilkcraft.raftworld.entity.RaftSharkEntity;
import com.wilkcraft.raftworld.init.ModBlocks;
import com.wilkcraft.raftworld.init.ModEntities;
import com.wilkcraft.raftworld.loot.ModLoot;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
  private static final String DEEP_DIAMOND_TAG = "raftworld_deep_diamond";
  private static final int DIAMOND_MIN_Y = 60;
  private static final int DIAMOND_MAX_Y = 70;
  private static final int DIAMOND_SPAWN_CHANCE = 4;
  private static final double DIAMOND_VERTICAL_PUSH = 0.035;
  private static final int SHARK_MIN_Y = 60;
  private static final int SHARK_MAX_Y = 80;
  private static final int SHARK_SPAWN_CHANCE = 30;
  private static final int MAX_SHARKS_NEAR_PLAYER = 6;
  private static final int SHARK_POD_MIN = 1;
  private static final int SHARK_POD_MAX = 3;

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
        if (item.getTags().contains(DEEP_DIAMOND_TAG)) {
          keepInDeepBand(item);
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
      if (nearbyItems < MAX_ITEMS_NEAR_PLAYER) {
        BlockPos base = player.blockPosition();
        int stage = ModLoot.getStage(player);
        if (stage >= ModLoot.STAGE_DEEP) {
          trySpawnDeepDiamond(level, base);
        }
        int x = base.getX() + 35 + RANDOM.nextInt(20);
        int z = base.getZ() - 25 + RANDOM.nextInt(51);
        int y = 90;
        BlockPos spawnPos = new BlockPos(x, y, z);
        if (!level.getFluidState(spawnPos).isEmpty()) {
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
      trySpawnShark(level, player);
    }
  }

  private static void keepInDeepBand(ItemEntity item) {
    double y = item.getY();
    var dm = item.getDeltaMovement();
    double vy = dm.y;
    if (y <= DIAMOND_MIN_Y) {
      vy = DIAMOND_VERTICAL_PUSH;
    } else if (y >= DIAMOND_MAX_Y) {
      vy = -DIAMOND_VERTICAL_PUSH;
    } else {
      vy *= 0.4;
    }
    item.setDeltaMovement(dm.x, vy, dm.z);
    item.hurtMarked = true;
  }

  private static void trySpawnDeepDiamond(ServerLevel level, BlockPos base) {
    if (RANDOM.nextInt(DIAMOND_SPAWN_CHANCE) != 0) {
      return;
    }
    int dx = base.getX() + 35 + RANDOM.nextInt(20);
    int dz = base.getZ() - 25 + RANDOM.nextInt(51);
    int dy = DIAMOND_MIN_Y + RANDOM.nextInt(DIAMOND_MAX_Y - DIAMOND_MIN_Y + 1);
    BlockPos diamondPos = new BlockPos(dx, dy, dz);
    if (level.getFluidState(diamondPos).isEmpty()) {
      return;
    }
    ItemEntity diamondEntity = new ItemEntity(
        level,
        dx + 0.5,
        dy + 0.5,
        dz + 0.5,
        new ItemStack(Items.DIAMOND));
    diamondEntity.addTag("raftworld_floating");
    diamondEntity.addTag(DEEP_DIAMOND_TAG);
    level.addFreshEntity(diamondEntity);
  }

  private static void trySpawnShark(ServerLevel level, ServerPlayer player) {
    int nearby = level.getEntitiesOfClass(
        RaftSharkEntity.class,
        player.getBoundingBox().inflate(48)).size();
    if (nearby >= MAX_SHARKS_NEAR_PLAYER) {
      return;
    }
    if (RANDOM.nextInt(SHARK_SPAWN_CHANCE) != 0) {
      return;
    }

    BlockPos base = player.blockPosition();
    int podX = base.getX() + 35 + RANDOM.nextInt(20);
    int podZ = base.getZ() - 25 + RANDOM.nextInt(51);
    int podY = SHARK_MIN_Y + RANDOM.nextInt(SHARK_MAX_Y - SHARK_MIN_Y + 1);
    BlockPos podPos = new BlockPos(podX, podY, podZ);
    if (level.getFluidState(podPos).isEmpty()) {
      return;
    }

    int podSize = SHARK_POD_MIN + RANDOM.nextInt(SHARK_POD_MAX - SHARK_POD_MIN + 1);
    int spawned = 0;
    for (int i = 0; i < podSize && (nearby + spawned) < MAX_SHARKS_NEAR_PLAYER; i++) {
      int x = podX + RANDOM.nextInt(7) - 3;
      int z = podZ + RANDOM.nextInt(7) - 3;
      int y = Math.max(SHARK_MIN_Y, Math.min(SHARK_MAX_Y, podY + RANDOM.nextInt(5) - 2));
      BlockPos pos = new BlockPos(x, y, z);
      if (level.getFluidState(pos).isEmpty()) {
        continue;
      }
      RaftSharkEntity shark = new RaftSharkEntity(ModEntities.RAFT_SHARK.get(), level);
      shark.moveTo(x + 0.5, y + 0.5, z + 0.5, RANDOM.nextFloat() * 360F, 0F);
      level.addFreshEntity(shark);
      spawned++;
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