package com.somapps.somai.net;

import java.util.UUID;

import com.somapps.somai.SomAI;
import com.somapps.somai.client.ClientCompanionStatusCache;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CompanionStatusResponsePayload(UUID companionId, String dimension, double x, double y, double z, float health, float maxHealth, boolean waiting,
        boolean pathing, String goal) implements CustomPacketPayload {
    private static final StreamCodec<RegistryFriendlyByteBuf, UUID> UUID_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, UUID::getMostSignificantBits,
            ByteBufCodecs.VAR_LONG, UUID::getLeastSignificantBits,
        UUID::new);

    public static final Type<CompanionStatusResponsePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SomAI.MODID, "companion_status_resp"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CompanionStatusResponsePayload> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> {
                UUID_CODEC.encode(buf, value.companionId());
                ByteBufCodecs.STRING_UTF8.encode(buf, value.dimension());
                ByteBufCodecs.DOUBLE.encode(buf, value.x());
                ByteBufCodecs.DOUBLE.encode(buf, value.y());
                ByteBufCodecs.DOUBLE.encode(buf, value.z());
                ByteBufCodecs.FLOAT.encode(buf, value.health());
                ByteBufCodecs.FLOAT.encode(buf, value.maxHealth());
                ByteBufCodecs.BOOL.encode(buf, value.waiting());
                ByteBufCodecs.BOOL.encode(buf, value.pathing());
                ByteBufCodecs.STRING_UTF8.encode(buf, value.goal());
            },
            buf -> new CompanionStatusResponsePayload(
                    UUID_CODEC.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.FLOAT.decode(buf),
                    ByteBufCodecs.FLOAT.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CompanionStatusResponsePayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ClientCompanionStatusCache.put(new ClientCompanionStatusCache.Status(payload.companionId(), payload.dimension(), payload.x(), payload.y(), payload.z(),
                    payload.health(), payload.maxHealth(), payload.waiting(), payload.pathing(), payload.goal(), System.currentTimeMillis()));
        });
    }
}
