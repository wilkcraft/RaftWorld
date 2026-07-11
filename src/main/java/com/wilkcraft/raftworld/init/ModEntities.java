package com.wilkcraft.raftworld.init;

import com.wilkcraft.raftworld.RaftWorld;
import com.wilkcraft.raftworld.entity.RaftSharkEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {
  public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE,
      RaftWorld.MODID);

  public static final DeferredHolder<EntityType<?>, EntityType<RaftSharkEntity>> RAFT_SHARK = ENTITY_TYPES
      .register("raft_shark", () -> EntityType.Builder
          .of(RaftSharkEntity::new, MobCategory.WATER_CREATURE)
          .sized(0.9F, 0.6F)
          .clientTrackingRange(10)
          .build("raft_shark"));
}