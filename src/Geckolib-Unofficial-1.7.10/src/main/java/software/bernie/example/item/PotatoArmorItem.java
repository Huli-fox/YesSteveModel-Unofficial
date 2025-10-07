package software.bernie.example.item;

import net.minecraft.entity.EntityLivingBase;
import software.bernie.example.GeckoLibMod;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;
import software.bernie.geckolib3.item.GeoArmorItem;

//This is an example of animated armor. Make sure to read the comments thoroughly and also check out PotatoArmorRenderer.
public class PotatoArmorItem extends GeoArmorItem implements IAnimatable {
    private AnimationFactory factory = new AnimationFactory(this);

    public PotatoArmorItem(ArmorMaterial materialIn, int renderIndexIn, int slot) {
        super(materialIn, renderIndexIn, slot);

        this.setCreativeTab(GeckoLibMod.getGeckolibItemGroup());
    }

    // Predicate runs every frame
    private <P extends IAnimatable> PlayState predicate(AnimationEvent<P> event) {
        // This is all the extradata this event carries. The livingentity is the entity
        // that's wearing the armor. The itemstack and equipmentslottype are self
        // explanatory.
        EntityLivingBase livingEntity = event.getExtraDataOfType(EntityLivingBase.class).get(0);

        // Always loop the animation but later on in this method we'll decide whether or
        // not to actually play it
        event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.potato_armor.new", true));

        return isFullSetWorn(livingEntity) ? PlayState.CONTINUE : PlayState.STOP;
        //return PlayState.STOP;
    }

    public boolean isFullSetWorn(EntityLivingBase entity) {
        for (int i = 1; i < 5; i++) {
            if (entity.getEquipmentInSlot(i) == null || !(entity.getEquipmentInSlot(i).getItem() instanceof PotatoArmorItem)) {
                return false;
            }
        }
        return true;
    }

    // All you need to do here is add your animation controllers to the
    // AnimationData
    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<PotatoArmorItem>(this, "controller", 20, this::predicate));
    }

    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }
}
