package com.wilkcraft.raftworld.loot;

import com.wilkcraft.raftworld.RaftWorld;
import com.wilkcraft.raftworld.init.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.List;
import java.util.Random;

@EventBusSubscriber(modid = RaftWorld.MODID)
public class ModLoot {

  public static final int STAGE_BASE = 0;
  public static final int STAGE_SEEDS = 1;
  public static final int STAGE_ORE = 2;
  public static final int STAGE_STONE_TOOLS = 3;
  public static final int STAGE_IRON_SMELT = 4;

  private static final String TAG_SEEDS_UNLOCKED = "raftworld_unlocked_seeds";
  private static final String TAG_ORE_UNLOCKED = "raftworld_unlocked_ore";
  private static final String TAG_STONE_UNLOCKED = "raftworld_unlocked_stone_tools";
  private static final String TAG_IRON_UNLOCKED = "raftworld_unlocked_iron";
  private static final String TAG_IRON_SMELTED_COUNT = "raftworld_iron_smelted_count";

  private static final int IRON_SMELT_TARGET = 5;

  private record LootEntry(Item item, int weight, int requiredStage) {
  }

  private static final List<LootEntry> ENTRIES = List.of(
      // --- Etapa base ---
      new LootEntry(Items.DIRT, 10, STAGE_BASE),
      new LootEntry(Items.OAK_PLANKS, 10, STAGE_BASE),
      new LootEntry(Items.STICK, 10, STAGE_BASE),
      new LootEntry(Items.ROTTEN_FLESH, 10, STAGE_BASE),
      new LootEntry(Items.STRING, 10, STAGE_BASE),
      new LootEntry(Items.OAK_SAPLING, 10, STAGE_BASE),

      // --- Etapa 1: raft_net conseguido ---
      new LootEntry(Items.WHEAT_SEEDS, 10, STAGE_SEEDS),
      new LootEntry(Items.POTATO, 5, STAGE_SEEDS),
      new LootEntry(Items.CARROT, 5, STAGE_SEEDS),

      // --- Etapa 2: 10 trigo + azada de madera ---
      new LootEntry(Items.COBBLESTONE, 10, STAGE_ORE),
      new LootEntry(Items.COAL, 8, STAGE_ORE),
      new LootEntry(Items.RAW_IRON, 6, STAGE_ORE),
      new LootEntry(Items.IRON_NUGGET, 8, STAGE_ORE),

      // --- Etapa 3: pico de piedra + hacha de piedra ---
      new LootEntry(Items.FLINT, 8, STAGE_STONE_TOOLS),
      new LootEntry(Items.CLAY_BALL, 8, STAGE_STONE_TOOLS),
      new LootEntry(Items.COD, 6, STAGE_STONE_TOOLS),
      new LootEntry(Items.SALMON, 4, STAGE_STONE_TOOLS),

      // --- Etapa 4: 5 lingotes de hierro fundidos ---
      new LootEntry(Items.REDSTONE, 8, STAGE_IRON_SMELT),
      new LootEntry(Items.LAPIS_LAZULI, 8, STAGE_IRON_SMELT),
      new LootEntry(Items.COPPER_INGOT, 6, STAGE_IRON_SMELT),
      new LootEntry(Items.GOLD_NUGGET, 4, STAGE_IRON_SMELT));

  public static ItemStack getRandomItem(int stage, Random random) {
    int totalWeight = 0;
    for (LootEntry entry : ENTRIES) {
      if (entry.requiredStage() <= stage) {
        totalWeight += entry.weight();
      }
    }
    if (totalWeight <= 0) {
      return new ItemStack(Items.DIRT);
    }
    int roll = random.nextInt(totalWeight);
    int cumulative = 0;
    for (LootEntry entry : ENTRIES) {
      if (entry.requiredStage() > stage) {
        continue;
      }
      cumulative += entry.weight();
      if (roll < cumulative) {
        return new ItemStack(entry.item());
      }
    }
    return new ItemStack(Items.DIRT);
  }

  public static int getStage(ServerPlayer player) {
    var data = player.getPersistentData();
    int stage = STAGE_BASE;
    if (data.getBoolean(TAG_SEEDS_UNLOCKED)) {
      stage = STAGE_SEEDS;
    }
    if (data.getBoolean(TAG_ORE_UNLOCKED)) {
      stage = STAGE_ORE;
    }
    if (data.getBoolean(TAG_STONE_UNLOCKED)) {
      stage = STAGE_STONE_TOOLS;
    }
    if (data.getBoolean(TAG_IRON_UNLOCKED)) {
      stage = STAGE_IRON_SMELT;
    }
    return stage;
  }

  public static void checkProgression(ServerPlayer player) {
    var data = player.getPersistentData();

    if (!data.getBoolean(TAG_SEEDS_UNLOCKED)) {
      boolean hasRaftNet = player.getInventory().countItem(ModItems.RAFT_NET.get()) > 0;
      if (hasRaftNet) {
        data.putBoolean(TAG_SEEDS_UNLOCKED, true);
        grantAdvancement(player, "raft_net_found");
      }
    }

    if (!data.getBoolean(TAG_ORE_UNLOCKED)) {
      boolean hasHoe = player.getInventory().countItem(Items.WOODEN_HOE) > 0;
      boolean hasWheat = player.getInventory().countItem(Items.WHEAT) >= 10;
      if (hasHoe && hasWheat) {
        data.putBoolean(TAG_ORE_UNLOCKED, true);
        grantAdvancement(player, "wheat_farming");
      }
    }

    if (!data.getBoolean(TAG_STONE_UNLOCKED)) {
      boolean hasPickaxe = player.getInventory().countItem(Items.STONE_PICKAXE) > 0;
      boolean hasAxe = player.getInventory().countItem(Items.STONE_AXE) > 0;
      if (hasPickaxe && hasAxe) {
        data.putBoolean(TAG_STONE_UNLOCKED, true);
        grantAdvancement(player, "stone_tools");
      }
    }
  }

  @SubscribeEvent
  public static void onItemSmelted(PlayerEvent.ItemSmeltedEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) {
      return;
    }
    var data = player.getPersistentData();
    if (data.getBoolean(TAG_IRON_UNLOCKED)) {
      return;
    }
    if (!event.getSmelting().is(Items.IRON_INGOT)) {
      return;
    }
    int count = data.getInt(TAG_IRON_SMELTED_COUNT) + event.getSmelting().getCount();
    data.putInt(TAG_IRON_SMELTED_COUNT, count);
    if (count >= IRON_SMELT_TARGET) {
      data.putBoolean(TAG_IRON_UNLOCKED, true);
      grantAdvancement(player, "iron_smelting");
    }
  }

  public static void grantAdvancement(ServerPlayer player, String path) {
    if (player.server == null) {
      return;
    }
    var holder = player.server.getAdvancements()
        .get(ResourceLocation.fromNamespaceAndPath(RaftWorld.MODID, path));
    if (holder == null) {
      return;
    }
    var progress = player.getAdvancements().getOrStartProgress(holder);
    if (progress.isDone()) {
      return;
    }
    for (String criterion : progress.getRemainingCriteria()) {
      player.getAdvancements().award(holder, criterion);
    }
  }
}