package software.bernie.example.client.renderer.entity;

import software.bernie.example.client.model.entity.LEModel;
import software.bernie.example.client.renderer.entity.layer.GeoExampleLayer;
import software.bernie.example.entity.GeoExampleEntityLayer;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

public class LERenderer extends GeoEntityRenderer<GeoExampleEntityLayer> {

    @SuppressWarnings("unchecked")
    public LERenderer() {
        super(new LEModel());
        this.addLayer(new GeoExampleLayer(this));
        this.shadowSize = 0.2f;
    }

}
