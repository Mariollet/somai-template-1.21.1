package com.somapps.somai.net;

import java.util.UUID;

import com.somapps.somai.CompanionActions;
import com.somapps.somai.SomAI;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record WhistleActionPayload(UUID companionId, Action action) implements CustomPacketPayload {
    public enum Action {
        RECALL,
        STOP,
        TOGGLE_WAIT
    }

        private static final StreamCodec<RegistryFriendlyByteBuf, UUID> UUID_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG, UUID::getMostSignificantBits,
                ByteBufCodecs.VAR_LONG, UUID::getLeastSignificantBits,
            UUID::new);

        public static final Type<WhistleActionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SomAI.MODID, "whistle_action"));

        public static final StreamCodec<RegistryFriendlyByteBuf, WhistleActionPayload> STREAM_CODEC = StreamCodec.composite(
            UUID_CODEC, WhistleActionPayload::companionId,
            ByteBufCodecs.STRING_UTF8, p -> p.action().name(),
            (uuid, actionName) -> new WhistleActionPayload(uuid, Action.valueOf(actionName)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WhistleActionPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> CompanionActions.handleAction(ctx, payload.companionId(), payload.action()));
    }
}
