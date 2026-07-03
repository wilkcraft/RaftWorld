package com.wilkcraft.raftworld.init;

import com.wilkcraft.raftworld.RaftWorld;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RaftWorld.MODID);

    public static final DeferredItem<BlockItem> RAFT_NET = ITEMS.registerSimpleBlockItem(
            "raft_net",
            ModBlocks.RAFT_NET);

    public static final DeferredItem<ArmorItem> ENCHANTED_TURTLE_HELMET = ITEMS.register(
            "enchanted_turtle_helmet",
            () -> new ArmorItem(
                    ((ArmorItem) Items.TURTLE_HELMET).getMaterial(),
                    ArmorItem.Type.HELMET,
                    new Item.Properties()
                            .rarity(Rarity.EPIC)
                            .stacksTo(1)
                            .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));
}