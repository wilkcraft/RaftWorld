package com.wilkcraft.raftworld.world;

import com.wilkcraft.raftworld.RaftWorld;
import com.wilkcraft.raftworld.command.RaftCommand;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

@EventBusSubscriber(modid = RaftWorld.MODID)
public class HostileSpawnHandler {
  @SubscribeEvent
  public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
    Mob mob = event.getEntity();
    if (!(mob instanceof Enemy)) {
      return;
    }
    if (!(mob.level() instanceof ServerLevel level)) {
      return;
    }
    if (!level.dimension().equals(RaftCommand.RAFT_DIM)) {
      return;
    }
    BlockPos pos = BlockPos.containing(event.getX(), event.getY(), event.getZ());
    if (!level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
      return;
    }
    boolean spawningInWater = level.getFluidState(pos).is(FluidTags.WATER);
    boolean spawningOnIsland = IslandPlacement.isInsideIsland(level.getSeed(), event.getX(), event.getZ());
    if (!spawningInWater && !spawningOnIsland) {
      event.setSpawnCancelled(true);
    }
  }
}