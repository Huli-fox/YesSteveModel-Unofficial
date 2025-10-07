package software.bernie.example.client.renderer.entity;

import software.bernie.example.client.model.entity.BikeModel;
import software.bernie.example.entity.BikeEntity;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

public class BikeGeoRenderer extends GeoEntityRenderer<BikeEntity> {
    public BikeGeoRenderer() {
        super(new BikeModel());
    }
}
