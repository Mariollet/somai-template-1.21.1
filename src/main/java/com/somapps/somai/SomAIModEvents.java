package com.somapps.somai;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = SomAI.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class SomAIModEvents {
    private SomAIModEvents() {
    }

    @SubscribeEvent
    public static void onEntityAttributes(net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event) {
        event.put(SomAI.HUMAN_COMPANION.get(), HumanCompanionEntity.createAttributes().build());
    }
}
