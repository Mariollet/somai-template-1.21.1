package com.somapps.somai;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.phys.Vec3;

import com.player2.playerengine.automaton.api.entity.IInteractionManagerProvider;
import com.player2.playerengine.automaton.api.entity.IInventoryProvider;
import com.player2.playerengine.automaton.api.entity.LivingEntityInteractionManager;
import com.player2.playerengine.automaton.api.entity.LivingEntityInventory;

public class HumanCompanionEntity extends TamableAnimal implements IInventoryProvider, IInteractionManagerProvider {
    private static final int INVENTORY_SIZE = 27;
    private static final String TAG_INVENTORY = "Inventory";
    private static final String TAG_PLAYERENGINE_INVENTORY = "PlayerEngineInventory";

    // "Idle" distance band around the owner: the companion won't stick to you.
    private static final float OWNER_IDLE_MIN_DISTANCE = 10.0f;
    private static final float OWNER_IDLE_MAX_DISTANCE = 20.0f;

    private final SimpleContainer inventory = new SimpleContainer(INVENTORY_SIZE);

    private final LivingEntityInventory livingEntityInventory = new LivingEntityInventory(this);
    private final LivingEntityInteractionManager interactionManager = new LivingEntityInteractionManager(this);

    public HumanCompanionEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.30)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0);
    }

    @Override
    public LivingEntityInventory getLivingInventory() {
        return livingEntityInventory;
    }

    @Override
    public LivingEntityInteractionManager getInteractionManager() {
        return interactionManager;
    }

    public BlockPos getOwnerBlockPos(ServerLevel level) {
        UUID ownerId = this.getOwnerUUID();
        if (ownerId == null) {
            return null;
        }
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null) {
            return null;
        }
        return owner.blockPosition();
    }

    public void sendOwnerMessage(ServerLevel level, String message) {
        UUID ownerId = this.getOwnerUUID();
        if (ownerId == null) {
            return;
        }
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null) {
            return;
        }
        SomAIChat.action(owner, message);
    }

    /** Insert stack into the companion inventory. Returns remainder (may be EMPTY). */
    public ItemStack insertIntoInventory(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack remaining = stack.copy();

        // First pass: merge into existing stacks
        for (int slot = 0; slot < INVENTORY_SIZE; slot++) {
            ItemStack existing = this.inventory.getItem(slot);
            if (existing.isEmpty()) {
                continue;
            }
            if (!ItemStack.isSameItemSameComponents(existing, remaining)) {
                continue;
            }

            int max = Math.min(existing.getMaxStackSize(), this.inventory.getMaxStackSize());
            int canAdd = max - existing.getCount();
            if (canAdd <= 0) {
                continue;
            }

            int toAdd = Math.min(canAdd, remaining.getCount());
            existing.grow(toAdd);
            remaining.shrink(toAdd);
            this.inventory.setItem(slot, existing);
            if (remaining.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }

        // Second pass: fill empty slots
        for (int slot = 0; slot < INVENTORY_SIZE; slot++) {
            ItemStack existing = this.inventory.getItem(slot);
            if (!existing.isEmpty()) {
                continue;
            }

            int max = Math.min(remaining.getMaxStackSize(), this.inventory.getMaxStackSize());
            ItemStack placed = remaining.copyWithCount(Math.min(max, remaining.getCount()));
            this.inventory.setItem(slot, placed);
            remaining.shrink(placed.getCount());
            if (remaining.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }

        return remaining;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new WaitWhenOrderedGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2, true));
        // Follow like wolves: start following when far, stop at a comfortable distance.
        // This gives a natural "idle" feel (10-20 blocks) without custom movement logic.
        this.goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.1, OWNER_IDLE_MAX_DISTANCE, OWNER_IDLE_MIN_DISTANCE));
        this.goalSelector.addGoal(8, new RandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));

        // Automatic combat: defend owner + fight nearby hostiles.
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, (new HurtByTargetGoal(this)).setAlertOthers());
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Monster.class, true));
    }

    /**
     * "Wait" behavior: when ordered to wait, stop moving but keep standing (no sitting pose).
     * We reuse TamableAnimal's ordered-to-sit flag as "wait" to keep persistence & existing logic.
     */
    private static final class WaitWhenOrderedGoal extends Goal {
        private final HumanCompanionEntity companion;

        private WaitWhenOrderedGoal(HumanCompanionEntity companion) {
            this.companion = companion;
            this.setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return this.companion.isTame() && this.companion.isOrderedToSit();
        }

        @Override
        public boolean canContinueToUse() {
            return this.companion.isTame() && this.companion.isOrderedToSit();
        }

        @Override
        public void start() {
            this.companion.getNavigation().stop();
            // Ensure we don't visually sit.
            this.companion.setInSittingPose(false);
        }

        @Override
        public void tick() {
            this.companion.getNavigation().stop();
            this.companion.setInSittingPose(false);
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!this.isTame()) {
            return InteractionResult.PASS;
        }

        if (!this.isOwnedBy(player)) {
            return InteractionResult.PASS;
        }

        if (player.isShiftKeyDown()) {
            this.setOrderedToSit(!this.isOrderedToSit());
            this.getNavigation().stop();
            if (player instanceof ServerPlayer serverPlayer) {
                SomAIChat.action(serverPlayer, this.isOrderedToSit() ? "Companion: wait." : "Companion: follow.");
            }
            return InteractionResult.CONSUME;
        }

        player.openMenu(createMenuProvider());
        return InteractionResult.CONSUME;
    }

    private MenuProvider createMenuProvider() {
        Component title = this.hasCustomName() ? this.getCustomName() : Component.translatable("entity.somai.human_companion");
        return new SimpleMenuProvider((containerId, playerInventory, player) -> ChestMenu.threeRows(containerId, playerInventory, this.inventory), title);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        ListTag items = new ListTag();
        for (int slot = 0; slot < INVENTORY_SIZE; slot++) {
            ItemStack stack = this.inventory.getItem(slot);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putByte("Slot", (byte) slot);
                stack.save(this.registryAccess(), itemTag);
                items.add(itemTag);
            }
        }
        tag.put(TAG_INVENTORY, items);

        ListTag peInventoryTag = new ListTag();
        this.livingEntityInventory.writeNbt(this.registryAccess(), peInventoryTag);
        tag.put(TAG_PLAYERENGINE_INVENTORY, peInventoryTag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        this.inventory.clearContent();
        if (tag.contains(TAG_INVENTORY, Tag.TAG_LIST)) {
            ListTag items = tag.getList(TAG_INVENTORY, Tag.TAG_COMPOUND);
            for (int i = 0; i < items.size(); i++) {
                CompoundTag itemTag = items.getCompound(i);
                int slot = itemTag.getByte("Slot") & 255;
                if (slot >= 0 && slot < INVENTORY_SIZE) {
                    this.inventory.setItem(slot, ItemStack.parseOptional(this.registryAccess(), itemTag));
                }
            }
        }

        if (tag.contains(TAG_PLAYERENGINE_INVENTORY, Tag.TAG_LIST)) {
            ListTag peInv = tag.getList(TAG_PLAYERENGINE_INVENTORY, Tag.TAG_COMPOUND);
            this.livingEntityInventory.readNbt(this.registryAccess(), peInv);
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level() instanceof ServerLevel level) {
            if (this.isTame()) {
                this.setPersistenceRequired();
            }

            // Keep PlayerEngine managers in sync.
            this.interactionManager.setWorld(level);
            this.interactionManager.update();
            this.livingEntityInventory.updateItems();
        }
    }

    @Override
    public void die(net.minecraft.world.damagesource.DamageSource damageSource) {
        if (!this.level().isClientSide) {
            for (int slot = 0; slot < INVENTORY_SIZE; slot++) {
                ItemStack stack = this.inventory.getItem(slot);
                if (!stack.isEmpty()) {
                    this.spawnAtLocation(stack);
                    this.inventory.setItem(slot, ItemStack.EMPTY);
                }
            }
        }
        super.die(damageSource);
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        // Not intended to be breedable; return a fresh instance to satisfy the contract.
        return SomAI.HUMAN_COMPANION.get().create(level);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Override
    public Vec3 getLeashOffset(float partialTick) {
        return new Vec3(0.0, this.getEyeHeight() * 0.6, this.getBbWidth() * 0.4);
    }
}
