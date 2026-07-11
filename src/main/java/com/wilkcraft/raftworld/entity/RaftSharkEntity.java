package com.wilkcraft.raftworld.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Dolphin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.UUID;

public class RaftSharkEntity extends Dolphin {

  private static final double ALERT_RADIUS = 16.0D;
  private static final double CIRCLE_RANGE = 20.0D;
  private static final double CIRCLE_RADIUS = 5.0D;
  private static final long GIVE_UP_TICKS = 30 * 20;
  private static final long SURFACE_IGNORE_TICKS = 20 * 20;
  private static final int EAT_DELAY_TICKS = 3 * 20;

  @Nullable
  private UUID ignoredPlayerId;
  private long ignoreUntilTick = -1;
  private int heldItemTicks = 0;

  public RaftSharkEntity(EntityType<? extends Dolphin> type, Level level) {
    super(type, level);
  }

  public static AttributeSupplier.Builder createAttributes() {
    return Dolphin.createAttributes()
        .add(Attributes.MAX_HEALTH, 16.0D)
        .add(Attributes.ATTACK_DAMAGE, 2.5D)
        .add(Attributes.MOVEMENT_SPEED, 1.3D)
        .add(Attributes.FOLLOW_RANGE, 24.0D);
  }

  @Override
  protected void registerGoals() {
    this.goalSelector.addGoal(1, new CircleSurfacedPlayerGoal());
    this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.4D, true));
    this.goalSelector.addGoal(3, new RandomSwimmingGoal(this, 1.0D, 10));
    this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
    this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

    this.targetSelector.addGoal(1,
        new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::canTargetPlayer));
  }

  private boolean canTargetPlayer(LivingEntity entity) {
    if (!(entity instanceof Player player)) {
      return false;
    }
    if (ignoredPlayerId != null
        && player.getUUID().equals(ignoredPlayerId)
        && this.level().getGameTime() < ignoreUntilTick) {
      return false;
    }
    return true;
  }

  @Override
  public void tick() {
    super.tick();
    if (!this.level().isClientSide) {
      clampUnderwater();
      handleHeldItemEating();
    }
  }

  private void clampUnderwater() {
    if (this.isInWater()) {
      return;
    }
    BlockPos.MutableBlockPos pos = this.blockPosition().mutable();
    int minY = this.level().getMinBuildHeight();
    while (!this.level().getFluidState(pos).is(FluidTags.WATER) && pos.getY() > minY) {
      pos.move(0, -1, 0);
    }
    this.setPos(this.getX(), pos.getY() + 0.1D, this.getZ());
    if (this.getDeltaMovement().y > 0) {
      this.setDeltaMovement(this.getDeltaMovement().x, 0, this.getDeltaMovement().z);
    }
  }

  private void handleHeldItemEating() {
    ItemStack held = this.getItemBySlot(EquipmentSlot.MAINHAND);
    if (held.isEmpty()) {
      heldItemTicks = 0;
      return;
    }
    heldItemTicks++;
    if (heldItemTicks >= EAT_DELAY_TICKS) {
      this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
      this.level().playSound(
          null,
          this.blockPosition(),
          SoundEvents.GENERIC_EAT,
          SoundSource.NEUTRAL,
          1.0F,
          1.0F + (this.random.nextFloat() - 0.5F) * 0.2F);
      heldItemTicks = 0;
    }
  }

  @Override
  public void setTarget(@Nullable LivingEntity target) {
    boolean isNewAggro = target instanceof Player && this.getTarget() != target;
    super.setTarget(target);
    if (isNewAggro && !this.level().isClientSide) {
      alertNearbySharks(target);
    }
  }

  private void alertNearbySharks(LivingEntity target) {
    AABB area = this.getBoundingBox().inflate(ALERT_RADIUS);
    for (RaftSharkEntity shark : this.level().getEntitiesOfClass(RaftSharkEntity.class, area)) {
      if (shark != this && shark.getTarget() == null) {
        shark.setTarget(target);
      }
    }
  }

  @Override
  protected InteractionResult mobInteract(Player player, InteractionHand hand) {
    return InteractionResult.PASS;
  }

  private class CircleSurfacedPlayerGoal extends Goal {
    private Player target;
    private double angle;
    private long outOfWaterSince = -1;

    CircleSurfacedPlayerGoal() {
      this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
      LivingEntity t = RaftSharkEntity.this.getTarget();
      if (!(t instanceof Player player) || !player.isAlive()
          || player.isSpectator() || player.isCreative()) {
        return false;
      }
      if (player.isInWater()) {
        return false;
      }
      return RaftSharkEntity.this.distanceToSqr(player) < CIRCLE_RANGE * CIRCLE_RANGE;
    }

    @Override
    public boolean canContinueToUse() {
      if (target == null || !target.isAlive() || target.isInWater()) {
        return false;
      }
      if (outOfWaterSince < 0) {
        return true;
      }
      return (RaftSharkEntity.this.level().getGameTime() - outOfWaterSince) < GIVE_UP_TICKS;
    }

    @Override
    public void start() {
      this.target = (Player) RaftSharkEntity.this.getTarget();
      this.angle = RaftSharkEntity.this.random.nextDouble() * Math.PI * 2;
      this.outOfWaterSince = RaftSharkEntity.this.level().getGameTime();
    }

    @Override
    public void stop() {
      boolean gaveUp = target != null && !target.isInWater();
      if (gaveUp) {
        RaftSharkEntity.this.ignoredPlayerId = target.getUUID();
        RaftSharkEntity.this.ignoreUntilTick = RaftSharkEntity.this.level().getGameTime() + SURFACE_IGNORE_TICKS;
        RaftSharkEntity.this.setTarget(null);
      }
      this.target = null;
      this.outOfWaterSince = -1;
    }

    @Override
    public void tick() {
      if (target == null) {
        return;
      }
      angle += 0.05D;
      double x = target.getX() + Math.cos(angle) * CIRCLE_RADIUS;
      double z = target.getZ() + Math.sin(angle) * CIRCLE_RADIUS;
      double y = RaftSharkEntity.this.getY();
      RaftSharkEntity.this.getNavigation().moveTo(x, y, z, 1.2D);
      RaftSharkEntity.this.getLookControl().setLookAt(target, 30.0F, 30.0F);
    }
  }
}