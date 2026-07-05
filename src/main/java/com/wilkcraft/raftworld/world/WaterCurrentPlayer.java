package com.wilkcraft.raftworld.world;

import com.wilkcraft.raftworld.command.RaftCommand;
import com.wilkcraft.raftworld.init.ModItems;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.world.item.Items;

@EventBusSubscriber
public class WaterCurrentPlayer {

    private static final double CURRENT_ACCELERATION = 0.0004;
    private static final double MAX_CURRENT = 0.012;
    private static final String CURRENT_TAG = "raftworld_current";

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {

        if (!(event.getEntity() instanceof ServerPlayer player))
            return;

        if (player.isSleeping())
            return;

        if (!player.level().dimension().equals(RaftCommand.RAFT_DIM))
            return;

        if (player.isCreative() || player.isSpectator())
            return;

        var head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (head.is(Items.TURTLE_HELMET) || head.is(ModItems.ENCHANTED_TURTLE_HELMET.get()))
            return;

        if (!player.isInWater()) {
            player.getPersistentData().putDouble(CURRENT_TAG, 0);
            return;
        }

        double current = player.getPersistentData().getDouble(CURRENT_TAG);

        current -= CURRENT_ACCELERATION;

        if (current < -MAX_CURRENT)
            current = -MAX_CURRENT;

        if (player.zza > 0) {
            current *= 0.75;
        }

        player.getPersistentData().putDouble(CURRENT_TAG, current);

        player.setDeltaMovement(
                current,
                player.getDeltaMovement().y,
                player.getDeltaMovement().z);

        player.hurtMarked = true;
    }
}