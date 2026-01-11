package com.somapps.somai.client;

import java.util.UUID;

import com.somapps.somai.CompanionWhistleItem;
import com.somapps.somai.SomAI;
import com.somapps.somai.net.CompanionStatusRequestPayload;
import com.somapps.somai.net.SomAINetwork;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = SomAI.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class SomAIClientEvents {
    private static int requestTick;

    private SomAIClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        ItemStack stack = mc.player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!(stack.getItem() instanceof CompanionWhistleItem)) {
            return;
        }

        UUID linked = CompanionWhistleItem.getLinkedCompanionId(stack);
        if (linked == null) {
            return;
        }

        requestTick++;
        if ((requestTick % 10) == 0) {
            SomAINetwork.sendToServer(new CompanionStatusRequestPayload(linked));
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        ItemStack stack = mc.player.getMainHandItem();
        if (!(stack.getItem() instanceof CompanionWhistleItem)) {
            return;
        }

        UUID linked = CompanionWhistleItem.getLinkedCompanionId(stack);
        if (linked == null) {
            return;
        }

        ClientCompanionStatusCache.Status status = ClientCompanionStatusCache.get(linked);
        if (status == null) {
            return;
        }

        GuiGraphics gg = event.getGuiGraphics();
        int x = 8;
        int y = 8;

        String line1 = "Companion: " + linked.toString().substring(0, 8);
        String line2 = "HP: " + (int) status.health() + "/" + (int) status.maxHealth() + (status.waiting() ? " (WAIT)" : "");
        String line3 = "Auto: " + (status.pathing() ? "pathing" : "idle") + (status.goal().isEmpty() ? "" : " - " + status.goal());

        gg.fill(x - 4, y - 4, x + 220, y + 38, 0x88000000);
        gg.drawString(mc.font, line1, x, y, 0xFFFFFF);
        gg.drawString(mc.font, line2, x, y + 12, 0xDDDDDD);
        gg.drawString(mc.font, line3, x, y + 24, 0xBBBBBB);
    }
}
