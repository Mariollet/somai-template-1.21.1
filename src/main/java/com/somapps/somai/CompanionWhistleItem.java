package com.somapps.somai;

import java.util.UUID;

import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import com.somapps.somai.menu.WhistleControlMenu;

public class CompanionWhistleItem extends Item {
    public static final String TAG_COMPANION_UUID = "SomAICompanionUuid";

    public CompanionWhistleItem(Properties properties) {
        super(properties);
    }

    public static UUID getLinkedCompanionId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag dataTag = customData.copyTag();
        return dataTag.hasUUID(TAG_COMPANION_UUID) ? dataTag.getUUID(TAG_COMPANION_UUID) : null;
    }

    public static void clearLinkedCompanion(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.remove(TAG_COMPANION_UUID));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.pass(stack);
        }

        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag dataTag = customData.copyTag();
        UUID companionId = dataTag.hasUUID(TAG_COMPANION_UUID) ? dataTag.getUUID(TAG_COMPANION_UUID) : null;

        // If the player is looking at their companion:
        // - If not linked: link.
        // - If already linked to that companion: open the control UI.
        HumanCompanionEntity lookedCompanion = getLookedAtCompanion(serverLevel, serverPlayer, 6.0);
        if (lookedCompanion != null && lookedCompanion.isOwnedBy(serverPlayer)) {
            UUID lookedId = lookedCompanion.getUUID();
            if (companionId == null || !lookedId.equals(companionId)) {
                CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putUUID(TAG_COMPANION_UUID, lookedId));
                SomAIChat.action(serverPlayer, "Whistle linked to companion.");
            }

            serverPlayer.openMenu(
                new SimpleMenuProvider((containerId, inv, p) -> new WhistleControlMenu(containerId, inv, lookedId),
                    Component.literal("SomAI Companion")),
                buf -> {
                    buf.writeBoolean(true);
                    buf.writeUUID(lookedId);
                });
            serverPlayer.getCooldowns().addCooldown(this, 10);
            return InteractionResultHolder.success(stack);
        }

        // Default: open the control UI. Actions are performed from the UI to keep behavior explicit and persistent.
        UUID targetId = companionId;
        serverPlayer.openMenu(
            new SimpleMenuProvider((containerId, inv, p) -> new WhistleControlMenu(containerId, inv, targetId),
                Component.literal("SomAI Companion")),
            buf -> {
                buf.writeBoolean(targetId != null);
                if (targetId != null) {
                    buf.writeUUID(targetId);
                }
            });
        serverPlayer.getCooldowns().addCooldown(this, 10);
        return InteractionResultHolder.success(stack);
    }

    private static HumanCompanionEntity getLookedAtCompanion(ServerLevel level, ServerPlayer player, double maxDistance) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(maxDistance));

        AABB box = player.getBoundingBox().expandTowards(player.getLookAngle().scale(maxDistance)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, player, start, end, box,
                e -> e instanceof HumanCompanionEntity && e.isAlive());

        if (hit == null) {
            return null;
        }

        if (hit.getType() != HitResult.Type.ENTITY) {
            return null;
        }

        Entity entity = hit.getEntity();
        return entity instanceof HumanCompanionEntity companion ? companion : null;
    }
}
