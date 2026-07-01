package com.wilkcraft.raftworld.block.entity;

import com.wilkcraft.raftworld.init.ModBlockEntities;
import com.wilkcraft.raftworld.menu.RaftNetMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class RaftNetBlockEntity extends BlockEntity implements MenuProvider {

  public static final int SLOT_COUNT = 9;

  private final SimpleContainer inventory = new SimpleContainer(SLOT_COUNT) {
    @Override
    public void setChanged() {
      super.setChanged();
      RaftNetBlockEntity.this.setChanged();
    }
  };

  public RaftNetBlockEntity(BlockPos pos, BlockState state) {
    super(ModBlockEntities.RAFT_NET.get(), pos, state);
  }

  public SimpleContainer getInventory() {
    return inventory;
  }

  @Override
  public Component getDisplayName() {
    return Component.translatable("container.raftworld.raft_net");
  }

  @Override
  public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
    return new RaftNetMenu(id, inventory, this);
  }

  @Override
  protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    super.saveAdditional(tag, registries);
    tag.put("Inventory", ContainerHelper.saveAllItems(new CompoundTag(), toList(), registries));
  }

  @Override
  protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    super.loadAdditional(tag, registries);
    net.minecraft.core.NonNullList<ItemStack> items = net.minecraft.core.NonNullList.withSize(SLOT_COUNT,
        ItemStack.EMPTY);
    ContainerHelper.loadAllItems(tag.getCompound("Inventory"), items, registries);
    for (int i = 0; i < SLOT_COUNT; i++) {
      inventory.setItem(i, items.get(i));
    }
  }

  private net.minecraft.core.NonNullList<ItemStack> toList() {
    net.minecraft.core.NonNullList<ItemStack> list = net.minecraft.core.NonNullList.withSize(SLOT_COUNT,
        ItemStack.EMPTY);
    for (int i = 0; i < SLOT_COUNT; i++)
      list.set(i, inventory.getItem(i));
    return list;
  }
}