package com.somapps.somai;

import java.util.UUID;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

public final class CompanionLocator {
    private CompanionLocator() {
    }

    public static HumanCompanionEntity findCompanion(MinecraftServer server, UUID companionId) {
        if (server == null || companionId == null) {
            return null;
        }
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(companionId);
            if (entity instanceof HumanCompanionEntity companion) {
                return companion;
            }
        }
        return null;
    }
}
