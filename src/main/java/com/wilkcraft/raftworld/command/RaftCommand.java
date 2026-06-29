package com.wilkcraft.raftworld.command;

import com.mojang.brigadier.CommandDispatcher;
import com.wilkcraft.raftworld.RaftWorld;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.registries.Registries;

public class RaftCommand {

  public static final ResourceKey<Level> RAFT_DIM = ResourceKey.create(
      Registries.DIMENSION,
      ResourceLocation.fromNamespaceAndPath(RaftWorld.MODID, "raft"));

  private static final BlockPos RAFT_CENTER = new BlockPos(0, 90, 0);

  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

    dispatcher.register(
        Commands.literal("startraft")
            .requires(s -> true)
            .executes(ctx -> {

              ServerPlayer player = ctx.getSource().getPlayerOrException();

              ServerLevel raftLevel = player.server.getLevel(RAFT_DIM);

              if (raftLevel == null)
                return 0;

              createRaft(raftLevel);

              raftLevel.setDefaultSpawnPos(
                  RAFT_CENTER.above(),
                  0);

              player.teleportTo(
                  raftLevel,
                  0.5,
                  91,
                  0.5,
                  0,
                  0);

              player.setRespawnPosition(
                  RAFT_DIM,
                  RAFT_CENTER.above(),
                  0,
                  true,
                  false);

              return 1;
            }));
  }

  private static void createRaft(ServerLevel level) {

    if (level.getBlockState(RAFT_CENTER).is(Blocks.OAK_PLANKS))
      return;

    for (int x = -1; x <= 1; x++) {
      for (int z = -1; z <= 1; z++) {

        level.setBlockAndUpdate(
            RAFT_CENTER.offset(x, 0, z),
            Blocks.OAK_PLANKS.defaultBlockState());

      }
    }

    BlockPos barrelPos = RAFT_CENTER.offset(1, 1, 1);

    level.setBlockAndUpdate(
        barrelPos,
        Blocks.BARREL.defaultBlockState());

    var blockEntity = level.getBlockEntity(barrelPos);

    if (blockEntity instanceof net.minecraft.world.level.block.entity.BarrelBlockEntity barrel) {
      barrel.setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.FISHING_ROD));
    }
  }
}