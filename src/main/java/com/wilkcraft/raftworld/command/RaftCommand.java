package com.wilkcraft.raftworld.command;

import com.mojang.brigadier.CommandDispatcher;
import com.wilkcraft.raftworld.RaftWorld;
import com.wilkcraft.raftworld.loot.ModLoot;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.Unbreakable;

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
              if (!player.level().dimension().equals(Level.OVERWORLD)) {
                ctx.getSource().sendFailure(
                    Component.translatable("raftworld.command.overworld_only"));
                return 0;
              }
              ServerLevel raftLevel = player.server.getLevel(RAFT_DIM);
              if (raftLevel == null)
                return 0;
              raftLevel.getChunkSource().getChunk(0, 0, ChunkStatus.FULL, true);
              createRaft(raftLevel);
              if (!raftLevel.getBlockState(RAFT_CENTER).is(Blocks.OAK_PLANKS)) {
                createRaft(raftLevel);
              }
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
              player.getPersistentData().putBoolean("raftworld_started", true);
              player.getPersistentData().putInt(
                  "raftworld_fast_items",
                  10);
              player.getAbilities().setWalkingSpeed(0.1F);
              restoreFlightAbility(player);
              player.onUpdateAbilities();
              player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
              player.removeEffect(MobEffects.BLINDNESS);
              ModLoot.grantAdvancement(player, "root");
              return 1;
            }));
  }

  @SuppressWarnings("deprecation")
  private static void restoreFlightAbility(ServerPlayer player) {
    boolean creativeLike = player.isCreative() || player.isSpectator();
    player.getAbilities().mayfly = creativeLike;
    if (!creativeLike) {
      player.getAbilities().flying = false;
    }
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
      net.minecraft.world.item.ItemStack rod = new net.minecraft.world.item.ItemStack(
          net.minecraft.world.item.Items.FISHING_ROD);
      rod.set(DataComponents.UNBREAKABLE, new Unbreakable(true));
      barrel.setItem(0, rod);
    }
  }
}