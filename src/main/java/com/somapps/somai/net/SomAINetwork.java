package com.somapps.somai.net;

import com.somapps.somai.SomAI;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

@EventBusSubscriber(modid = SomAI.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class SomAINetwork {
    private SomAINetwork() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(SomAI.MODID).versioned("1");

        registrar.playToServer(WhistleActionPayload.TYPE, WhistleActionPayload.STREAM_CODEC, WhistleActionPayload::handle);
        registrar.playToServer(WhistleSummonPayload.TYPE, WhistleSummonPayload.STREAM_CODEC, WhistleSummonPayload::handle);
        registrar.playToServer(WhistleClearLinkPayload.TYPE, WhistleClearLinkPayload.STREAM_CODEC, WhistleClearLinkPayload::handle);
        registrar.playToServer(WhistleRunCommandPayload.TYPE, WhistleRunCommandPayload.STREAM_CODEC, WhistleRunCommandPayload::handle);
        registrar.playToServer(CompanionStatusRequestPayload.TYPE, CompanionStatusRequestPayload.STREAM_CODEC, CompanionStatusRequestPayload::handle);
        registrar.playToClient(CompanionStatusResponsePayload.TYPE, CompanionStatusResponsePayload.STREAM_CODEC, CompanionStatusResponsePayload::handle);
    }

    public static void sendToServer(CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    public static void sendToPlayer(net.minecraft.server.level.ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }
}
