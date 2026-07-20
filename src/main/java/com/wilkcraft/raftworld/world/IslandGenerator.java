package com.wilkcraft.raftworld.world;

import com.wilkcraft.raftworld.RaftWorld;
import com.wilkcraft.raftworld.command.RaftCommand;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

@EventBusSubscriber(modid = RaftWorld.MODID)
public class IslandGenerator {
  private static final int WATER_TOP_Y = 90;
  private static final int CELL_SIZE_CHUNKS = 32;
  private static final int ISLAND_CHANCE_PER_CELL = 3;
  private static final int HARMONICS = 4;
  private static final int SAND_DEPTH = 3;
  private static final int ORE_ROLL_BOUND = 220;
  private static final int IRON_ROLL_MAX = 1;
  private static final int COAL_ROLL_MAX = 4;
  private static final int NEIGHBOR_CHECK_RADIUS_CHUNKS = 2;

  private enum IslandSize {
    SMALL(4.5, 3.0, 11, 55, 4, 1),
    MEDIUM(8.0, 3.5, 17, 42, 6, 3),
    BIG(12.0, 4.5, 24, 25, 9, 6);

    final double radiusBase;
    final double radiusVariance;
    final int maxThickness;
    final int minFloatY;
    final int bumpMax;
    final int maxTrees;

    IslandSize(double radiusBase, double radiusVariance, int maxThickness, int minFloatY, int bumpMax, int maxTrees) {
      this.radiusBase = radiusBase;
      this.radiusVariance = radiusVariance;
      this.maxThickness = maxThickness;
      this.minFloatY = minFloatY;
      this.bumpMax = bumpMax;
      this.maxTrees = maxTrees;
    }

    static IslandSize roll(Random random) {
      int r = random.nextInt(100);
      if (r < 50)
        return SMALL; // 50%
      if (r < 85)
        return MEDIUM; // 35%
      return BIG; // 15%
    }
  }

  @SubscribeEvent
  public static void onChunkLoad(ChunkEvent.Load event) {
    if (!event.isNewChunk())
      return;
    if (!(event.getLevel() instanceof ServerLevel level))
      return;
    if (!level.dimension().equals(RaftCommand.RAFT_DIM))
      return;

    ChunkAccess chunk = event.getChunk();
    int chunkX = chunk.getPos().x;
    int chunkZ = chunk.getPos().z;
    long seed = level.getSeed();

    for (int tx = chunkX - NEIGHBOR_CHECK_RADIUS_CHUNKS; tx <= chunkX + NEIGHBOR_CHECK_RADIUS_CHUNKS; tx++) {
      for (int tz = chunkZ - NEIGHBOR_CHECK_RADIUS_CHUNKS; tz <= chunkZ + NEIGHBOR_CHECK_RADIUS_CHUNKS; tz++) {
        if (!isIslandTargetChunk(seed, tx, tz))
          continue;
        Random islandRandom = new Random(Objects.hash(seed, tx, tz, 55221L));
        paintIslandPortion(level, chunk, chunkX, chunkZ, tx, tz, islandRandom);
      }
    }
  }

  private static boolean isIslandTargetChunk(long seed, int chunkX, int chunkZ) {
    int cellX = Math.floorDiv(chunkX, CELL_SIZE_CHUNKS);
    int cellZ = Math.floorDiv(chunkZ, CELL_SIZE_CHUNKS);
    Random cellRandom = new Random(Objects.hash(seed, cellX, cellZ, 91827L));
    if (cellRandom.nextInt(ISLAND_CHANCE_PER_CELL) != 0)
      return false;
    int targetLocalX = cellRandom.nextInt(CELL_SIZE_CHUNKS);
    int targetLocalZ = cellRandom.nextInt(CELL_SIZE_CHUNKS);
    int targetChunkX = cellX * CELL_SIZE_CHUNKS + targetLocalX;
    int targetChunkZ = cellZ * CELL_SIZE_CHUNKS + targetLocalZ;
    return chunkX == targetChunkX && chunkZ == targetChunkZ;
  }

  private static void paintIslandPortion(ServerLevel level, ChunkAccess chunk, int chunkX, int chunkZ,
      int islandChunkX, int islandChunkZ, Random random) {

    boolean isHomeChunk = (islandChunkX == chunkX && islandChunkZ == chunkZ);
    int islandBaseX = islandChunkX * 16;
    int islandBaseZ = islandChunkZ * 16;
    int centerLocalX = 5 + random.nextInt(6);
    int centerLocalZ = 5 + random.nextInt(6);
    double centerGlobalX = islandBaseX + centerLocalX;
    double centerGlobalZ = islandBaseZ + centerLocalZ;
    IslandSize size = IslandSize.roll(random);
    double baseRadius = size.radiusBase + random.nextDouble() * size.radiusVariance;
    double[] amp = new double[HARMONICS];
    double[] freq = new double[HARMONICS];
    double[] phase = new double[HARMONICS];
    for (int i = 0; i < HARMONICS; i++) {
      amp[i] = (0.6 + random.nextDouble() * 1.1) / (i + 1);
      freq[i] = i + 2;
      phase[i] = random.nextDouble() * Math.PI * 2;
    }

    double maxPossibleRadius = baseRadius;
    for (double a : amp)
      maxPossibleRadius += a;
    int chunkBaseX = chunkX * 16;
    int chunkBaseZ = chunkZ * 16;
    double closestDx = Math.max(0, Math.max(chunkBaseX - centerGlobalX, centerGlobalX - (chunkBaseX + 15)));
    double closestDz = Math.max(0, Math.max(chunkBaseZ - centerGlobalZ, centerGlobalZ - (chunkBaseZ + 15)));
    if (Math.sqrt(closestDx * closestDx + closestDz * closestDz) > maxPossibleRadius) {
      return;
    }
    List<int[]> treePositions = new ArrayList<>();

    List<int[]> villagerSpots = new ArrayList<>();
    int treesPlaced = 0;
    for (int lx = 0; lx < 16; lx++) {
      for (int lz = 0; lz < 16; lz++) {
        double gx = chunkBaseX + lx;
        double gz = chunkBaseZ + lz;
        double dx = gx - centerGlobalX;
        double dz = gz - centerGlobalZ;
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < 0.001)
          dist = 0.001;
        double angle = Math.atan2(dz, dx);
        double radius = baseRadius;
        for (int i = 0; i < HARMONICS; i++) {
          radius += amp[i] * Math.sin(angle * freq[i] + phase[i]);
        }
        if (radius < 2.0)
          radius = 2.0;
        if (dist > radius)
          continue;
        double edge = Math.max(0.0, Math.min(1.0, dist / radius));
        boolean sandyColumn = edge > 0.75;
        double heightProfile = Math.cos(edge * Math.PI / 2.0);
        int bump = (int) Math.round(heightProfile * size.bumpMax);
        int topY = WATER_TOP_Y + 1 + bump;
        double thicknessProfile = Math.sqrt(Math.max(0.0, 1.0 - edge * edge));
        int thickness = 2 + (int) Math.round(thicknessProfile * size.maxThickness);
        int bottomY = Math.max(size.minFloatY, topY - thickness);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos((int) gx, 0, (int) gz);
        var surfaceBlock = sandyColumn ? Blocks.SAND.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState();
        var subSurfaceBlock = sandyColumn ? Blocks.SAND.defaultBlockState() : Blocks.DIRT.defaultBlockState();
        for (int y = bottomY; y <= topY; y++) {
          pos.setY(y);
          if (y == topY) {
            chunk.setBlockState(pos, surfaceBlock, false);
          } else if (y >= topY - 2) {
            chunk.setBlockState(pos, subSurfaceBlock, false);
          } else if (sandyColumn && y >= topY - 2 - SAND_DEPTH) {
            chunk.setBlockState(pos, Blocks.SANDSTONE.defaultBlockState(), false);
          } else {
            chunk.setBlockState(pos, rollUndergroundBlock(random), false);
          }
        }
        if (!sandyColumn) {
          pos.setY(topY + 1);
          double roll = random.nextDouble();
          if (roll < 0.20) {
            chunk.setBlockState(pos, Blocks.SHORT_GRASS.defaultBlockState(), false);
          } else if (roll < 0.25) {
            chunk.setBlockState(pos, Blocks.DEAD_BUSH.defaultBlockState(), false);
          }
          int wx = (int) gx;
          int wz = (int) gz;
          if (isHomeChunk && edge < 0.7) {
            villagerSpots.add(new int[] { wx, topY + 1, wz });
          }

          boolean canopyFitsInChunk = lx >= 2 && lx <= 13 && lz >= 2 && lz <= 13;
          if (canopyFitsInChunk && treesPlaced < size.maxTrees && dist < radius * 0.35 && random.nextDouble() < 0.5) {
            boolean tooClose = false;
            for (int[] tp : treePositions) {
              int ddx = tp[0] - wx;
              int ddz = tp[1] - wz;
              if (ddx * ddx + ddz * ddz < 25) {
                tooClose = true;
                break;
              }
            }
            if (!tooClose) {
              placeAcaciaTree(chunk, wx, topY + 1, wz, random);
              treePositions.add(new int[] { wx, wz });
              treesPlaced++;
            }
          }
        }
      }
    }
    if (isHomeChunk) {
      trySpawnVillagers(level, size, villagerSpots, random);
    }
  }

  private static BlockState rollUndergroundBlock(Random random) {
    int roll = random.nextInt(ORE_ROLL_BOUND);
    if (roll < IRON_ROLL_MAX) {
      return Blocks.IRON_ORE.defaultBlockState();
    } else if (roll < COAL_ROLL_MAX) {
      return Blocks.COAL_ORE.defaultBlockState();
    }
    return Blocks.STONE.defaultBlockState();
  }

  private static void trySpawnVillagers(ServerLevel level, IslandSize size, List<int[]> villagerSpots,
      Random random) {
    if (villagerSpots.isEmpty()) {
      return;
    }
    int count = switch (size) {
      case SMALL -> random.nextInt(2); // 0-1
      case MEDIUM -> random.nextInt(3); // 0-2
      case BIG -> 1 + random.nextInt(3); // 1-3
    };
    List<int[]> available = new ArrayList<>(villagerSpots);
    for (int i = 0; i < count && !available.isEmpty(); i++) {
      int index = random.nextInt(available.size());
      int[] spot = available.remove(index);
      Villager villager = new Villager(EntityType.VILLAGER, level);
      villager.moveTo(spot[0] + 0.5, spot[1], spot[2] + 0.5, random.nextFloat() * 360F, 0F);
      villager.finalizeSpawn(level, level.getCurrentDifficultyAt(new BlockPos(spot[0], spot[1], spot[2])),
          net.minecraft.world.entity.MobSpawnType.CHUNK_GENERATION, null);
      level.addFreshEntity(villager);
    }
  }

  private static void placeAcaciaTree(ChunkAccess chunk, int x, int baseY, int z, Random random) {
    int trunkHeight = 4 + random.nextInt(2);
    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, baseY, z);
    for (int i = 0; i < trunkHeight; i++) {
      pos.setY(baseY + i);
      chunk.setBlockState(pos, Blocks.ACACIA_LOG.defaultBlockState(), false);
    }
    int lowerLeafY = baseY + trunkHeight - 1;
    int upperLeafY = baseY + trunkHeight;
    int topLeafY = baseY + trunkHeight + 1;

    for (int lx = -2; lx <= 2; lx++) {
      for (int lz = -2; lz <= 2; lz++) {
        boolean isCorner = Math.abs(lx) == 2 && Math.abs(lz) == 2;
        if (isCorner && random.nextDouble() < 0.4) {
          continue;
        }
        pos.set(x + lx, lowerLeafY, z + lz);
        if (chunk.getBlockState(pos).is(Blocks.ACACIA_LOG)) {
          continue;
        }
        chunk.setBlockState(pos, Blocks.ACACIA_LEAVES.defaultBlockState(), false);
      }
    }

    for (int lx = -2; lx <= 2; lx++) {
      for (int lz = -2; lz <= 2; lz++) {
        boolean isCorner = Math.abs(lx) == 2 && Math.abs(lz) == 2;
        boolean isFarEdge = Math.abs(lx) + Math.abs(lz) >= 3;
        if (isCorner || (isFarEdge && random.nextDouble() < 0.6)) {
          continue;
        }
        pos.set(x + lx, upperLeafY, z + lz);
        chunk.setBlockState(pos, Blocks.ACACIA_LEAVES.defaultBlockState(), false);
      }
    }

    pos.set(x, topLeafY, z);
    chunk.setBlockState(pos, Blocks.ACACIA_LEAVES.defaultBlockState(), false);
    for (int lx = -1; lx <= 1; lx++) {
      for (int lz = -1; lz <= 1; lz++) {
        if (lx == 0 && lz == 0) {
          continue;
        }
        if (random.nextDouble() < 0.5) {
          continue;
        }
        pos.set(x + lx, topLeafY, z + lz);
        chunk.setBlockState(pos, Blocks.ACACIA_LEAVES.defaultBlockState(), false);
      }
    }
  }
}