package com.somapps.somai.net;

import com.somapps.somai.CompanionActions;
import com.somapps.somai.SomAI;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record WhistleClearLinkPayload() implements CustomPacketPayload {
    public static final Type<WhistleClearLinkPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SomAI.MODID, "whistle_clear_link"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WhistleClearLinkPayload> STREAM_CODEC = StreamCodec.unit(new WhistleClearLinkPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WhistleClearLinkPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> CompanionActions.handleClearLink(ctx));
    }
}
