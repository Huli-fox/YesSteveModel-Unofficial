package software.bernie.example.client.model.entity;

import net.minecraft.util.ResourceLocation;
import software.bernie.example.entity.GeoNpcEntity;
import software.bernie.geckolib3.GeckoLib;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class GeoNpcModel extends AnimatedGeoModel<GeoNpcEntity> {
    @Override
    public ResourceLocation getAnimationFileLocation(GeoNpcEntity entity) {
        return new ResourceLocation("custom", "geo_npc.animation.json");
    }

    @Override
    public ResourceLocation getModelLocation(GeoNpcEntity entity) {
        return new ResourceLocation(GeckoLib.ModID, "geo/npc.geo.json");
    }

    @Override
    public ResourceLocation getTextureLocation(GeoNpcEntity entity) {
        return new ResourceLocation(GeckoLib.ModID, "textures/model/entity/geo_npc.png");
    }
}
