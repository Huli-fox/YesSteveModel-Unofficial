package software.bernie.example.client.model.entity; //The package our class is located in

//Imports of the classes used in this class description

import net.minecraft.util.ResourceLocation;
import software.bernie.example.entity.GeoExampleEntity;
import software.bernie.geckolib3.GeckoLib;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.processor.IBone;
import software.bernie.geckolib3.model.AnimatedTickingGeoModel;
import software.bernie.geckolib3.model.provider.data.EntityModelData;

//The model class is derived from the AnimatedTickingGeoModel with the template argument(<>) set to the previously made entity class
public class ExampleEntityModel extends AnimatedTickingGeoModel<GeoExampleEntity> {

    //This method should return a ResourceLocation for the animation file based on the entity state
    @Override
    public ResourceLocation getAnimationFileLocation(GeoExampleEntity entity) {
        return new ResourceLocation(GeckoLib.ModID, "animations/bat.animation.json");
    }

    //This method should return a ResourceLocation for the model file based on the entity state
    @Override
    public ResourceLocation getModelLocation(GeoExampleEntity entity) {
        return new ResourceLocation(GeckoLib.ModID, "geo/bat.geo.json");
    }

    //This method should return a ResourceLocation for the texture file based on the entity state
    @Override
    public ResourceLocation getTextureLocation(GeoExampleEntity entity) {
        return new ResourceLocation(GeckoLib.ModID, "textures/model/entity/bat.png");
    }

    //This function allows you to change model properties each frame before render
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void setLivingAnimations(GeoExampleEntity entity, Integer uniqueID, AnimationEvent animationEvent) {
        super.setLivingAnimations(entity, uniqueID, animationEvent); //We call the super-function
        IBone head = this.getAnimationProcessor().getBone("head"); //Then we take the head bone
        //We get the model data for an entity
        EntityModelData extraData = (EntityModelData) animationEvent.getExtraDataOfType(EntityModelData.class).get(0);
        //And we set the head bone rotation to the interpolated pitch and yaw rotations of an entity
        head.setRotationX(extraData.headPitch * ((float) Math.PI / 180F));
        head.setRotationY(extraData.netHeadYaw * ((float) Math.PI / 180F));
    }
}
