package com.somapps.somai;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class SomAIChat {
    private static final Component PREFIX = Component.literal("[SomAI] ")
            .withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD);

    private SomAIChat() {
    }

    /**
     * Sends a visible chat message (not actionbar) to the player.
     */
    public static void action(ServerPlayer player, String message) {
        if (player == null) {
            return;
        }

        Component body = Component.literal(message).withStyle(ChatFormatting.AQUA);
        player.displayClientMessage(Component.empty().append(PREFIX).append(body), false);
    }

    public static void warn(ServerPlayer player, String message) {
        if (player == null) {
            return;
        }

        Component body = Component.literal(message).withStyle(ChatFormatting.GOLD);
        player.displayClientMessage(Component.empty().append(PREFIX).append(body), false);
    }

    public static void error(ServerPlayer player, String message) {
        if (player == null) {
            return;
        }

        Component body = Component.literal(message).withStyle(ChatFormatting.RED);
        player.displayClientMessage(Component.empty().append(PREFIX).append(body), false);
    }
}
