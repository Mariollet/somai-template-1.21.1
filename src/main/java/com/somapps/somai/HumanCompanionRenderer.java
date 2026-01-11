package com.somapps.somai;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class HumanCompanionRenderer extends HumanoidMobRenderer<HumanCompanionEntity, HumanoidModel<HumanCompanionEntity>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("somai", "textures/entity/human_companion.png");

    public HumanCompanionRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(HumanCompanionEntity entity) {
        return TEXTURE;
    }
}
