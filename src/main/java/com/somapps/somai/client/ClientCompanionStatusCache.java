package com.somapps.somai.client;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientCompanionStatusCache {
    public record Status(UUID companionId, String dimension, double x, double y, double z, float health, float maxHealth, boolean waiting, boolean pathing,
            String goal, long updatedAtMillis) {
    }

    private static final Map<UUID, Status> CACHE = new ConcurrentHashMap<>();

    private ClientCompanionStatusCache() {
    }

    public static void put(Status status) {
        if (status == null || status.companionId() == null) {
            return;
        }
        CACHE.put(status.companionId(), status);
    }

    public static Status get(UUID companionId) {
        return companionId == null ? null : CACHE.get(companionId);
    }
}
