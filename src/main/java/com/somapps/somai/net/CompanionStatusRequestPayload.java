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

public record CompanionStatusRequestPayload(UUID companionId) implements CustomPacketPayload {
    private static final StreamCodec<RegistryFriendlyByteBuf, UUID> UUID_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, UUID::getMostSignificantBits,
            ByteBufCodecs.VAR_LONG, UUID::getLeastSignificantBits,
        UUID::new);

    public static final Type<CompanionStatusRequestPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SomAI.MODID, "companion_status_req"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CompanionStatusRequestPayload> STREAM_CODEC = StreamCodec.composite(
        UUID_CODEC, CompanionStatusRequestPayload::companionId,
            CompanionStatusRequestPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CompanionStatusRequestPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> CompanionActions.handleStatusRequest(ctx, payload.companionId()));
    }
}
