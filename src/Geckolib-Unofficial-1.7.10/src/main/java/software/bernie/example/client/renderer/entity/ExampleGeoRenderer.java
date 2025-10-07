package software.bernie.example.client.renderer.entity; //The package our class is located in

//Imports of the classes used in this class description

import software.bernie.example.client.model.entity.ExampleEntityModel;
import software.bernie.example.entity.GeoExampleEntity;
import software.bernie.geckolib3.core.util.Color;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

//The model class is derived from the GeoEntityRenderer with the template argument(<>) set to the previously made entity class
public class ExampleGeoRenderer extends GeoEntityRenderer<GeoExampleEntity> {

    //The constructor defines the model which will be used
    public ExampleGeoRenderer() {
        super(new ExampleEntityModel()); // Here we pass the previously made model into the super-constructor
    }

    //This function specifiec the color/tint of the model rendered
    //By default the model does not color red when hurt. If you want change that, you can do it like that
    public Color getRenderColor(GeoExampleEntity animatable, float partialTicks) {
        if (animatable.hurtTime > 0 || animatable.deathTime > 0) { //If the hurt or death timers are greater then zero, then
            return Color.ofRGBA(255, 153, 153, 255); //We say that the model should be colored red (you can set here any RGBA color)
        }
        return Color.ofRGBA(255, 255, 255, 255); //Else we return the regular white color (and again, here can be any color you want)
    }
}
