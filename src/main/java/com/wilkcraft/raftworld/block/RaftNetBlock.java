package com.wilkcraft.raftworld.block;

import javax.annotation.Nullable;
import com.wilkcraft.raftworld.block.entity.RaftNetBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.server.level.ServerPlayer;

public class RaftNetBlock extends BaseEntityBlock {
  public static final MapCodec<RaftNetBlock> CODEC = simpleCodec(RaftNetBlock::new);

  @Override
  protected MapCodec<? extends BaseEntityBlock> codec() {
    return CODEC;
  }

  public RaftNetBlock(BlockBehaviour.Properties properties) {
    super(properties);
  }

  @Override
  protected RenderShape getRenderShape(BlockState state) {
    return RenderShape.MODEL;
  }

  @Override
  @Nullable
  public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
    return new RaftNetBlockEntity(pos, state);
  }

  @Override
  protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
      Player player, BlockHitResult hitResult) {
    if (!level.isClientSide) {
      var be = level.getBlockEntity(pos);
      if (be instanceof RaftNetBlockEntity netBe && player instanceof ServerPlayer sp) {
        sp.openMenu(netBe);
      }
    }
    return InteractionResult.sidedSuccess(level.isClientSide);
  }

  @Override
  protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
    if (!state.is(newState.getBlock()) && !level.isClientSide) {
      var be = level.getBlockEntity(pos);
      if (be instanceof RaftNetBlockEntity netBe) {
        Containers.dropContents(level, pos, netBe.getInventory());
      }
    }
    super.onRemove(state, level, pos, newState, movedByPiston);
  }
}