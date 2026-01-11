package com.somapps.somai.net;

import com.somapps.somai.CompanionActions;
import com.somapps.somai.SomAI;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record WhistleSummonPayload(boolean summon) implements CustomPacketPayload {
    public static final Type<WhistleSummonPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SomAI.MODID, "whistle_summon"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WhistleSummonPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL, WhistleSummonPayload::summon,
        WhistleSummonPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WhistleSummonPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> CompanionActions.handleSummon(ctx));
    }
}
