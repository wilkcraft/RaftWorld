package com.wilkcraft.raftworld.world;

import com.wilkcraft.raftworld.RaftWorld;
import com.wilkcraft.raftworld.command.RaftCommand;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.monster.Monster;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import java.util.List;
import java.util.Random;

@EventBusSubscriber(modid = RaftWorld.MODID)
public class IslandHostileSpawner {
  private static final Random RANDOM = new Random();
  private static final int CHECK_INTERVAL_TICKS = 40;
  private static final int SEARCH_RADIUS_BLOCKS = 40;
  private static final int MIN_DISTANCE_FROM_PLAYER = 6;
  private static final int MAX_HOSTILES_NEAR_PLAYER = 8;
  private static final int SPAWN_ATTEMPTS_PER_PLAYER = 4;
  private static final int MAX_LIGHT_LEVEL = 7;
  private static final int SURFACE_SEARCH_TOP_Y = 112;
  private static final int SURFACE_SEARCH_BOTTOM_Y = 85;

  private record SpawnEntry(EntityType<? extends Monster> type, int weight) {
  }

  private static final List<SpawnEntry> ENTRIES = List.of(
      new SpawnEntry(EntityType.ZOMBIE, 55),
      new SpawnEntry(EntityType.SKELETON, 30),
      new SpawnEntry(EntityType.SPIDER, 30),
      new SpawnEntry(EntityType.CREEPER, 20));

  @SubscribeEvent
  public static void onWorldTick(LevelTickEvent.Post event) {
    if (!(event.getLevel() instanceof ServerLevel level)) {
      return;
    }
    if (!level.dimension().equals(RaftCommand.RAFT_DIM)) {
      return;
    }
    if (level.getGameTime() % CHECK_INTERVAL_TICKS != 0) {
      return;
    }
    if (!level.isNight()) {
      return;
    }

    if (level.getDifficulty() == Difficulty.PEACEFUL) {
      return;
    }
    for (ServerPlayer player : level.players()) {
      if (player.isCreative() || player.isSpectator()) {
        continue;
      }
      trySpawnAroundPlayer(level, player);
    }
  }

  private static void trySpawnAroundPlayer(ServerLevel level, ServerPlayer player) {
    int nearby = level.getEntitiesOfClass(
        Monster.class,
        player.getBoundingBox().inflate(SEARCH_RADIUS_BLOCKS)).size();
    if (nearby >= MAX_HOSTILES_NEAR_PLAYER) {
      return;
    }
    BlockPos base = player.blockPosition();
    for (int i = 0; i < SPAWN_ATTEMPTS_PER_PLAYER && nearby < MAX_HOSTILES_NEAR_PLAYER; i++) {
      int x = base.getX() + RANDOM.nextInt(SEARCH_RADIUS_BLOCKS * 2 + 1) - SEARCH_RADIUS_BLOCKS;
      int z = base.getZ() + RANDOM.nextInt(SEARCH_RADIUS_BLOCKS * 2 + 1) - SEARCH_RADIUS_BLOCKS;

      if (!IslandPlacement.isInsideIsland(level.getSeed(), x + 0.5, z + 0.5)) {
        continue;
      }
      int y = findIslandSurfaceY(level, x, z);
      if (y == Integer.MIN_VALUE) {
        continue;
      }
      BlockPos spawnPos = new BlockPos(x, y, z);
      if (player.distanceToSqr(x + 0.5, y, z + 0.5) < (double) MIN_DISTANCE_FROM_PLAYER * MIN_DISTANCE_FROM_PLAYER) {
        continue;
      }
      if (level.getMaxLocalRawBrightness(spawnPos) > MAX_LIGHT_LEVEL) {
        continue;
      }
      if (spawnMonster(level, spawnPos)) {
        nearby++;
      }
    }
  }

  private static int findIslandSurfaceY(ServerLevel level, int x, int z) {
    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, SURFACE_SEARCH_TOP_Y, z);
    for (int y = SURFACE_SEARCH_TOP_Y; y >= SURFACE_SEARCH_BOTTOM_Y; y--) {
      pos.setY(y);
      var state = level.getBlockState(pos);
      if (state.isAir()) {
        continue;
      }
      if (state.is(net.minecraft.tags.BlockTags.LEAVES) || state.is(net.minecraft.tags.BlockTags.LOGS)) {
        continue;
      }
      if (!level.getFluidState(pos).isEmpty()) {
        continue;
      }
      pos.setY(y + 1);
      if (!level.getBlockState(pos).isAir()) {
        continue;
      }

      return y + 1;
    }
    return Integer.MIN_VALUE;
  }

  private static boolean spawnMonster(ServerLevel level, BlockPos spawnPos) {
    EntityType<? extends Monster> type = rollType();
    Monster mob = type.create(level);
    if (mob == null) {
      return false;
    }
    mob.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, RANDOM.nextFloat() * 360F, 0F);
    if (!mob.checkSpawnObstruction(level)) {
      return false;
    }
    EventHooks.finalizeMobSpawn(mob, level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.NATURAL, null);
    return level.addFreshEntity(mob);
  }

  private static EntityType<? extends Monster> rollType() {
    int totalWeight = 0;
    for (SpawnEntry entry : ENTRIES) {
      totalWeight += entry.weight();
    }
    int roll = RANDOM.nextInt(totalWeight);
    int cumulative = 0;
    for (SpawnEntry entry : ENTRIES) {
      cumulative += entry.weight();
      if (roll < cumulative) {
        return entry.type();
      }
    }
    return EntityType.ZOMBIE;
  }
}