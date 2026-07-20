package com.wilkcraft.raftworld.world;

import java.util.Objects;
import java.util.Random;

public final class IslandPlacement {
  private static final int CELL_SIZE_CHUNKS = 32;
  private static final int ISLAND_CHANCE_PER_CELL = 3;
  private static final int HARMONICS = 4;
  private static final int SEARCH_RADIUS_CHUNKS = 2;

  public enum IslandSize {
    SMALL(4.5, 3.0),
    MEDIUM(8.0, 3.5),
    BIG(12.0, 4.5);

    final double radiusBase;
    final double radiusVariance;

    IslandSize(double radiusBase, double radiusVariance) {
      this.radiusBase = radiusBase;
      this.radiusVariance = radiusVariance;
    }

    static IslandSize roll(Random random) {
      int r = random.nextInt(100);
      if (r < 50)
        return SMALL;
      if (r < 85)
        return MEDIUM;
      return BIG;
    }
  }

  private IslandPlacement() {
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

  public static boolean isInsideIsland(long seed, double x, double z) {
    int chunkX = Math.floorDiv((int) Math.floor(x), 16);
    int chunkZ = Math.floorDiv((int) Math.floor(z), 16);
    for (int tx = chunkX - SEARCH_RADIUS_CHUNKS; tx <= chunkX + SEARCH_RADIUS_CHUNKS; tx++) {
      for (int tz = chunkZ - SEARCH_RADIUS_CHUNKS; tz <= chunkZ + SEARCH_RADIUS_CHUNKS; tz++) {
        if (!isIslandTargetChunk(seed, tx, tz)) {
          continue;
        }
        if (isInsideIslandAt(seed, tx, tz, x, z)) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean isInsideIslandAt(long seed, int islandChunkX, int islandChunkZ, double x, double z) {
    Random islandRandom = new Random(Objects.hash(seed, islandChunkX, islandChunkZ, 55221L));
    int islandBaseX = islandChunkX * 16;
    int islandBaseZ = islandChunkZ * 16;
    int centerLocalX = 5 + islandRandom.nextInt(6);
    int centerLocalZ = 5 + islandRandom.nextInt(6);
    double centerGlobalX = islandBaseX + centerLocalX;
    double centerGlobalZ = islandBaseZ + centerLocalZ;
    IslandSize size = IslandSize.roll(islandRandom);
    double baseRadius = size.radiusBase + islandRandom.nextDouble() * size.radiusVariance;
    double[] amp = new double[HARMONICS];
    double[] freq = new double[HARMONICS];
    double[] phase = new double[HARMONICS];
    for (int i = 0; i < HARMONICS; i++) {
      amp[i] = (0.6 + islandRandom.nextDouble() * 1.1) / (i + 1);
      freq[i] = i + 2;
      phase[i] = islandRandom.nextDouble() * Math.PI * 2;
    }
    double dx = x - centerGlobalX;
    double dz = z - centerGlobalZ;
    double dist = Math.sqrt(dx * dx + dz * dz);
    if (dist < 0.001) {
      dist = 0.001;
    }
    double angle = Math.atan2(dz, dx);
    double radius = baseRadius;
    for (int i = 0; i < HARMONICS; i++) {
      radius += amp[i] * Math.sin(angle * freq[i] + phase[i]);
    }
    if (radius < 2.0) {
      radius = 2.0;
    }
    return dist <= radius;
  }
}