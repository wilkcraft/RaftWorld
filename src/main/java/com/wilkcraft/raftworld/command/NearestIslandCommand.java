package com.wilkcraft.raftworld.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import java.util.Objects;
import java.util.Random;

public class NearestIslandCommand {
  private static final int WATER_TOP_Y = 90;
  private static final int CELL_SIZE_CHUNKS = 32;
  private static final int ISLAND_CHANCE_PER_CELL = 3;
  private static final int SEARCH_RADIUS_CELLS = 6;
  private static final int CHEATS_PERMISSION_LEVEL = 2;

  private enum IslandSize {
    SMALL(4, "pequeña"),
    MEDIUM(6, "mediana"),
    LARGE(9, "grande");

    final int bumpMax;
    final String label;

    IslandSize(int bumpMax, String label) {
      this.bumpMax = bumpMax;
      this.label = label;
    }

    static IslandSize roll(Random random) {
      int r = random.nextInt(100);
      if (r < 50)
        return SMALL;
      if (r < 85)
        return MEDIUM;
      return LARGE;
    }
  }

  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(
        Commands.literal("nearestisland")
            .requires(NearestIslandCommand::canUseCommand)
            .executes(ctx -> findNearestIsland(ctx, null))
            .then(Commands.literal("small").executes(ctx -> findNearestIsland(ctx, IslandSize.SMALL)))
            .then(Commands.literal("medium").executes(ctx -> findNearestIsland(ctx, IslandSize.MEDIUM)))
            .then(Commands.literal("large").executes(ctx -> findNearestIsland(ctx, IslandSize.LARGE))));
  }

  private static boolean canUseCommand(CommandSourceStack source) {
    if (source.getEntity() instanceof ServerPlayer player) {
      if (player.isCreative() || player.isSpectator()) {
        return true;
      }
    }
    return source.hasPermission(CHEATS_PERMISSION_LEVEL);
  }

  private static int findNearestIsland(CommandContext<CommandSourceStack> ctx, IslandSize filter)
      throws CommandSyntaxException {
    ServerPlayer player = ctx.getSource().getPlayerOrException();
    ServerLevel level = (ServerLevel) player.level();
    long seed = level.getSeed();
    int playerChunkX = player.blockPosition().getX() >> 4;
    int playerChunkZ = player.blockPosition().getZ() >> 4;
    int playerCellX = Math.floorDiv(playerChunkX, CELL_SIZE_CHUNKS);
    int playerCellZ = Math.floorDiv(playerChunkZ, CELL_SIZE_CHUNKS);
    BlockPos best = null;
    IslandSize bestSize = null;
    double bestDistSq = Double.MAX_VALUE;
    for (int cx = playerCellX - SEARCH_RADIUS_CELLS; cx <= playerCellX + SEARCH_RADIUS_CELLS; cx++) {
      for (int cz = playerCellZ - SEARCH_RADIUS_CELLS; cz <= playerCellZ + SEARCH_RADIUS_CELLS; cz++) {
        Random cellRandom = new Random(Objects.hash(seed, cx, cz, 91827L));
        if (cellRandom.nextInt(ISLAND_CHANCE_PER_CELL) != 0)
          continue;
        int targetLocalX = cellRandom.nextInt(CELL_SIZE_CHUNKS);
        int targetLocalZ = cellRandom.nextInt(CELL_SIZE_CHUNKS);
        int targetChunkX = cx * CELL_SIZE_CHUNKS + targetLocalX;
        int targetChunkZ = cz * CELL_SIZE_CHUNKS + targetLocalZ;
        Random islandRandom = new Random(Objects.hash(seed, targetChunkX, targetChunkZ, 55221L));
        int centerX = 5 + islandRandom.nextInt(6);
        int centerZ = 5 + islandRandom.nextInt(6);
        IslandSize rolledSize = IslandSize.roll(islandRandom);
        if (filter != null && rolledSize != filter) {
          continue;
        }
        int blockX = targetChunkX * 16 + centerX;
        int blockZ = targetChunkZ * 16 + centerZ;
        double dx = blockX - player.getX();
        double dz = blockZ - player.getZ();
        double distSq = dx * dx + dz * dz;
        if (distSq < bestDistSq) {
          bestDistSq = distSq;
          best = new BlockPos(blockX, WATER_TOP_Y + 3 + rolledSize.bumpMax, blockZ);
          bestSize = rolledSize;
        }
      }
    }
    if (best == null) {
      ctx.getSource().sendFailure(Component.literal(
          "No se encontró ninguna isla" + (filter != null ? " " + filter.label : "")
              + " cerca. Sube SEARCH_RADIUS_CELLS."));
      return 0;
    }
    String tpCmd = "/execute in raftworld:raft run tp @s "
        + best.getX() + " " + best.getY() + " " + best.getZ();
    MutableComponent msg = Component.literal("Isla " + bestSize.label + " más cercana: ")
        .append(Component.literal(
            "[" + best.getX() + ", " + best.getY() + ", " + best.getZ() + "]")
            .withStyle(style -> style
                .withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, tpCmd))
                .withHoverEvent(new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    Component.literal("Clic para teletransportarte")))));
    ctx.getSource().sendSuccess(() -> msg, false);
    return 1;
  }
}