package com.wilkcraft.raftworld.menu;

import com.wilkcraft.raftworld.block.entity.RaftNetBlockEntity;
import com.wilkcraft.raftworld.init.ModBlocks;
import com.wilkcraft.raftworld.init.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class RaftNetMenu extends AbstractContainerMenu {
  private static final int CONTAINER_SLOTS = RaftNetBlockEntity.SLOT_COUNT;

  private final RaftNetBlockEntity blockEntity;
  private final ContainerLevelAccess access;

  public RaftNetMenu(int id, Inventory playerInv, RaftNetBlockEntity be) {
    super(ModMenus.RAFT_NET.get(), id);
    this.blockEntity = be;
    this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());

    for (int row = 0; row < 3; row++) {
      for (int col = 0; col < 3; col++) {
        this.addSlot(new Slot(be.getInventory(), row * 3 + col, 62 + col * 18, 17 + row * 18));
      }
    }

    for (int row = 0; row < 3; row++) {
      for (int col = 0; col < 9; col++) {
        this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
      }
    }

    for (int col = 0; col < 9; col++) {
      this.addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
    }
  }

  public RaftNetMenu(int id, Inventory playerInv) {
    this(id, playerInv, new RaftNetBlockEntity(BlockPos.ZERO, ModBlocks.RAFT_NET.get().defaultBlockState()));
  }

  @Override
  public boolean stillValid(Player player) {
    return stillValid(access, player, blockEntity.getBlockState().getBlock());
  }

  @Override
  public ItemStack quickMoveStack(Player player, int index) {
    ItemStack result = ItemStack.EMPTY;
    Slot slot = this.slots.get(index);
    if (slot != null && slot.hasItem()) {
      ItemStack stackInSlot = slot.getItem();
      result = stackInSlot.copy();
      if (index < CONTAINER_SLOTS) {
        if (!this.moveItemStackTo(stackInSlot, CONTAINER_SLOTS, this.slots.size(), true)) {
          return ItemStack.EMPTY;
        }
      } else {
        if (!this.moveItemStackTo(stackInSlot, 0, CONTAINER_SLOTS, false)) {
          return ItemStack.EMPTY;
        }
      }
      if (stackInSlot.isEmpty()) {
        slot.set(ItemStack.EMPTY);
      } else {
        slot.setChanged();
      }
    }
    return result;
  }
}