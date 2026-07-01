package com.wilkcraft.raftworld.init;

import com.wilkcraft.raftworld.RaftWorld;
import com.wilkcraft.raftworld.menu.RaftNetMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;

public class ModMenus {
  public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, RaftWorld.MODID);

  public static final DeferredHolder<MenuType<?>, MenuType<RaftNetMenu>> RAFT_NET = MENUS.register("raft_net",
      () -> IMenuTypeExtension.create((windowId, inv, buf) -> new RaftNetMenu(windowId, inv)));
}