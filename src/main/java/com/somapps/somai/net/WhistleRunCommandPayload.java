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

public record WhistleRunCommandPayload(UUID companionId, String command) implements CustomPacketPayload {
    private static final StreamCodec<RegistryFriendlyByteBuf, UUID> UUID_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, UUID::getMostSignificantBits,
            ByteBufCodecs.VAR_LONG, UUID::getLeastSignificantBits,
        UUID::new);

    public static final Type<WhistleRunCommandPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SomAI.MODID, "whistle_run_command"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WhistleRunCommandPayload> STREAM_CODEC = StreamCodec.composite(
        UUID_CODEC, WhistleRunCommandPayload::companionId,
            ByteBufCodecs.STRING_UTF8, WhistleRunCommandPayload::command,
            WhistleRunCommandPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WhistleRunCommandPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> CompanionActions.handleRunCommand(ctx, payload.companionId(), payload.command()));
    }
}
