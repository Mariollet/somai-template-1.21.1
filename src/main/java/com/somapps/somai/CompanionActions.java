package com.somapps.somai;

import java.util.UUID;
import java.util.Set;

import com.somapps.somai.net.CompanionStatusResponsePayload;
import com.somapps.somai.net.SomAINetwork;
import com.somapps.somai.net.WhistleActionPayload;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.somapps.somai.menu.WhistleControlMenu;

public final class CompanionActions {
    private CompanionActions() {
    }

    public static void handleSummon(IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack whistle = findHeldWhistle(player);
        if (whistle == null || whistle.isEmpty()) {
            return;
        }

        UUID linked = CompanionWhistleItem.getLinkedCompanionId(whistle);
        if (linked != null) {
            HumanCompanionEntity existing = CompanionLocator.findCompanion(player.server, linked);
            if (existing != null && existing.isAlive()) {
                if (existing.isOwnedBy(player)) {
                    SomAIChat.warn(player, "This whistle is already linked to a companion.");
                } else {
                    SomAIChat.error(player, "This whistle is linked to a companion you don't own.");
                }
            } else {
                SomAIChat.warn(player, "Linked companion not found (unloaded or dead).");
            }
            return;
        }

        ServerLevel level = player.serverLevel();
        HumanCompanionEntity companion = SomAI.HUMAN_COMPANION.get().create(level);
        if (companion == null) {
            SomAIChat.error(player, "Failed to spawn companion.");
            return;
        }

        companion.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0f);
        companion.tame(player);
        companion.setCustomName(Component.literal("Buddy"));
        companion.setCustomNameVisible(true);
        companion.setOrderedToSit(false);
        level.addFreshEntity(companion);

        UUID newId = companion.getUUID();
        CompanionWhistleItem.clearLinkedCompanion(whistle);
        net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, whistle,
            t -> t.putUUID(CompanionWhistleItem.TAG_COMPANION_UUID, newId));

        SomAIChat.action(player, "Companion summoned.");

        // Refresh the open UI (if any) with the new companion id.
        player.openMenu(
            new SimpleMenuProvider((containerId, inv, p) -> new WhistleControlMenu(containerId, inv, newId),
                Component.literal("SomAI Companion")),
            buf -> {
                buf.writeBoolean(true);
                buf.writeUUID(newId);
            });
    }

    public static void handleClearLink(IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack whistle = findHeldWhistle(player);
        if (whistle == null || whistle.isEmpty()) {
            return;
        }

        UUID linked = CompanionWhistleItem.getLinkedCompanionId(whistle);
        if (linked == null) {
            return;
        }

        CompanionWhistleItem.clearLinkedCompanion(whistle);
        SomAIChat.action(player, "Whistle link cleared.");

        player.openMenu(
            new SimpleMenuProvider((containerId, inv, p) -> new WhistleControlMenu(containerId, inv, (java.util.UUID) null),
                Component.literal("SomAI Companion")),
            buf -> buf.writeBoolean(false));
    }

    public static void handleAction(IPayloadContext ctx, UUID companionId, WhistleActionPayload.Action action) {
        if (!(ctx.player() instanceof ServerPlayer player)) {
            return;
        }
        if (companionId == null || action == null) {
            return;
        }

        if (!isHoldingLinkedWhistle(player, companionId)) {
            return;
        }

        HumanCompanionEntity companion = CompanionLocator.findCompanion(player.server, companionId);
        if (companion == null || !companion.isAlive()) {
            SomAIChat.warn(player, "Companion not found (unloaded or dead).");
            return;
        }
        if (!companion.isOwnedBy(player)) {
            SomAIChat.error(player, "You don't own this companion.");
            return;
        }

        switch (action) {
            case RECALL -> recall(player, companion);
            case STOP -> PlayerEngineBridge.stop(player, companion);
            case TOGGLE_WAIT -> {
                boolean next = !companion.isOrderedToSit();
                companion.setOrderedToSit(next);
                SomAIChat.action(player, next ? "Companion waiting." : "Companion following.");
            }
        }
    }

    public static void handleRunCommand(IPayloadContext ctx, UUID companionId, String command) {
        if (!(ctx.player() instanceof ServerPlayer player)) {
            return;
        }
        if (companionId == null) {
            return;
        }

        if (!isHoldingLinkedWhistle(player, companionId)) {
            return;
        }

        command = command == null ? "" : command.trim();
        if (command.isEmpty()) {
            return;
        }

        HumanCompanionEntity companion = CompanionLocator.findCompanion(player.server, companionId);
        if (companion == null || !companion.isAlive()) {
            SomAIChat.warn(player, "Companion not found (unloaded or dead).");
            return;
        }
        if (!companion.isOwnedBy(player)) {
            SomAIChat.error(player, "You don't own this companion.");
            return;
        }

        if (!PlayerEngineBridge.isAvailable()) {
            SomAIChat.error(player, "PlayerEngine not available.");
            return;
        }

        PlayerEngineBridge.executeCommand(player, companion, command);
    }

    public static void handleStatusRequest(IPayloadContext ctx, UUID companionId) {
        if (!(ctx.player() instanceof ServerPlayer player)) {
            return;
        }
        if (companionId == null) {
            return;
        }

        if (!isHoldingLinkedWhistle(player, companionId)) {
            return;
        }

        HumanCompanionEntity companion = CompanionLocator.findCompanion(player.server, companionId);
        if (companion == null || !companion.isAlive() || !companion.isOwnedBy(player)) {
            return;
        }

        PlayerEngineBridge.AutomatonStatus auto = PlayerEngineBridge.getAutomatonStatus(companion);

        ServerLevel level = (ServerLevel) companion.level();
        String dim = level.dimension().location().toString();

        SomAINetwork.sendToPlayer(player, new CompanionStatusResponsePayload(companionId, dim, companion.getX(), companion.getY(), companion.getZ(), companion.getHealth(),
            companion.getMaxHealth(), companion.isOrderedToSit(), auto.pathing(), auto.goal()));
    }

    private static void recall(ServerPlayer owner, HumanCompanionEntity companion) {
        ServerLevel target = owner.serverLevel();

        companion.setOrderedToSit(false);

        if (companion.level() != target) {
            try {
                companion.teleportTo(target, owner.getX(), owner.getY(), owner.getZ(), Set.<RelativeMovement>of(), owner.getYRot(), owner.getXRot());
                SomAIChat.action(owner, "Companion recalled (cross-dimension)." );
            } catch (Throwable t) {
                SomAIChat.error(owner, "Recall failed: " + t.getClass().getSimpleName());
            }
            return;
        }

        double distSqr = companion.distanceToSqr(owner);
        if (distSqr > (50.0 * 50.0)) {
            companion.teleportTo(owner.getX(), owner.getY(), owner.getZ());
            SomAIChat.action(owner, "Companion recalled (>50 blocks -> teleported)." );
            return;
        }

        companion.getNavigation().moveTo(owner, 1.2);
        SomAIChat.action(owner, "Companion is on the way (pathfinding)." );
    }

    private static boolean isHoldingLinkedWhistle(ServerPlayer player, UUID companionId) {
        var main = player.getMainHandItem();
        if (main.getItem() instanceof CompanionWhistleItem && companionId.equals(CompanionWhistleItem.getLinkedCompanionId(main))) {
            return true;
        }
        var off = player.getOffhandItem();
        return off.getItem() instanceof CompanionWhistleItem && companionId.equals(CompanionWhistleItem.getLinkedCompanionId(off));
    }

    private static ItemStack findHeldWhistle(ServerPlayer player) {
        var main = player.getMainHandItem();
        if (main.getItem() instanceof CompanionWhistleItem) {
            return main;
        }
        var off = player.getOffhandItem();
        if (off.getItem() instanceof CompanionWhistleItem) {
            return off;
        }
        return null;
    }
}
