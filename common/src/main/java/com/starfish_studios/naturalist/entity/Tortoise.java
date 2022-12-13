package com.starfish_studios.naturalist.entity;

import com.starfish_studios.naturalist.registry.NaturalistEntityTypes;
import com.starfish_studios.naturalist.registry.NaturalistTags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

public class Tortoise extends TamableAnimal implements IAnimatable {
    public static final int MAX_MOSS_LEVEL = 3;
    private final AnimationFactory factory = new AnimationFactory(this);
    private static final Ingredient TEMPT_ITEMS = Ingredient.of(NaturalistTags.ItemTags.TORTOISE_TEMPT_ITEMS);
    private static final EntityDataAccessor<Integer> VARIANT_ID = SynchedEntityData.defineId(Tortoise.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MOSS_LEVEL = SynchedEntityData.defineId(Tortoise.class, EntityDataSerializers.INT);

    public Tortoise(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.2f).add(Attributes.MAX_HEALTH, 8.0).add(Attributes.ATTACK_DAMAGE, 2.0);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return NaturalistEntityTypes.TORTOISE.get().create(level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new TemptGoal(this, 0.6, TEMPT_ITEMS, false));
        this.goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.0, 10.0f, 5.0f, false));
        this.goalSelector.addGoal(4, new BreedGoal(this, 0.8));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0f));
    }

    @Override
    public boolean canMate(Animal otherAnimal) {
        if (!this.isTame()) {
            return false;
        }
        if (!(otherAnimal instanceof Tortoise tortoise)) {
            return false;
        }
        return tortoise.isTame() && super.canMate(otherAnimal);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return TEMPT_ITEMS.test(stack);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        InteractionResult interactionResult;
        ItemStack itemStack = player.getItemInHand(hand);
        if (this.level.isClientSide) {
            if (this.isTame() && this.isOwnedBy(player)) {
                return InteractionResult.SUCCESS;
            }
            if (this.isFood(itemStack) && (this.getHealth() < this.getMaxHealth() || !this.isTame())) {
                return InteractionResult.SUCCESS;
            }
            if (itemStack.getItem() instanceof ShearsItem) {
                return InteractionResult.CONSUME;
            }
            return InteractionResult.PASS;
        }
        if (this.getMossLevel() > 0) {
            this.level.playSound(null, this, SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 1.0F, 1.0F);
            for(int j = 0; j < this.getMossLevel(); ++j) {
                ItemEntity itemEntity = this.spawnAtLocation(Items.MOSS_CARPET, 1);
                if (itemEntity != null) {
                    itemEntity.setDeltaMovement(itemEntity.getDeltaMovement().add((this.random.nextFloat() - this.random.nextFloat()) * 0.1F, this.random.nextFloat() * 0.05F, (this.random.nextFloat() - this.random.nextFloat()) * 0.1F));
                }
            }
            this.setMossLevel(0);
            this.gameEvent(GameEvent.SHEAR, player);
            itemStack.hurtAndBreak(1, player, (playerx) -> {
                playerx.broadcastBreakEvent(hand);
            });
            return InteractionResult.SUCCESS;
        } else if (this.isTame()) {
            if (this.isOwnedBy(player)) {
                if (this.isFood(itemStack) && this.getHealth() < this.getMaxHealth()) {
                    this.usePlayerItem(player, hand, itemStack);
                    this.heal(3.0F);
                    return InteractionResult.CONSUME;
                }
                InteractionResult interactionResult2 = super.mobInteract(player, hand);
                if (!interactionResult2.consumesAction() || this.isBaby()) {
                    this.setOrderedToSit(!this.isOrderedToSit());
                }
                return interactionResult2;
            }
        } else if (this.isFood(itemStack)) {
            this.usePlayerItem(player, hand, itemStack);
            if (this.random.nextInt(3) == 0) {
                this.tame(player);
                this.setOrderedToSit(true);
                this.level.broadcastEntityEvent(this, (byte)7);
            } else {
                this.level.broadcastEntityEvent(this, (byte)6);
            }
            this.setPersistenceRequired();
            return InteractionResult.CONSUME;
        }
        if ((interactionResult = super.mobInteract(player, hand)).consumesAction()) {
            this.setPersistenceRequired();
        }
        return interactionResult;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level.isClientSide) {
            if (this.getMossLevel() < MAX_MOSS_LEVEL && this.random.nextInt(500) == 0) {
                this.setMossLevel(this.getMossLevel() + 1);
            }
        }
    }

    // ENTITY DATA

    public int getMossLevel() {
        return Mth.clamp(this.entityData.get(MOSS_LEVEL), 0, MAX_MOSS_LEVEL);
    }

    public void setMossLevel(int mossLevel) {
        this.entityData.set(MOSS_LEVEL, mossLevel);
    }

    public int getVariant() {
        return Mth.clamp(this.entityData.get(VARIANT_ID), 0, 2);
    }

    public void setVariant(int variant) {
        this.entityData.set(VARIANT_ID, variant);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(VARIANT_ID, 0);
        this.entityData.define(MOSS_LEVEL, 0);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("Variant", this.getVariant());
        compound.putInt("MossLevel", this.getMossLevel());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.setVariant(compound.getInt("Variant"));
        this.setVariant(compound.getInt("MossLevel"));
    }

    @Override
    public void registerControllers(AnimationData animationData) {
        animationData.addAnimationController(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private <T extends IAnimatable> PlayState predicate(AnimationEvent<T> event) {
        if (this.isInSittingPose()) {
            event.getController().setAnimation(new AnimationBuilder().addAnimation("tortoise.retreat", true));
            return PlayState.CONTINUE;
        } else if (this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6) {
            event.getController().setAnimation(new AnimationBuilder().addAnimation("tortoise.walk", true));
            event.getController().setAnimationSpeed(2.0D);
            return PlayState.CONTINUE;
        }
        event.getController().markNeedsReload();
        return PlayState.STOP;
    }

    @Override
    public AnimationFactory getFactory() {
        return factory;
    }
}
