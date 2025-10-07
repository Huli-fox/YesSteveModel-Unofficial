package software.bernie.example.block.tile;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import software.bernie.geckolib3.collision.ComplexBB;
import software.bernie.geckolib3.collision.IBBHolder;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

public class DiagonalTileEntity extends TileEntity implements IAnimatable, IBBHolder {
    private final AnimationFactory factory = new AnimationFactory(this);
    public AxisAlignedBB boundingBox;

    @SuppressWarnings("unchecked")
    private <E extends TileEntity & IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        event.getController().transitionLengthTicks = 0;
        //event.getController().setAnimation(new AnimationBuilder().addAnimation("Botarium.anim.deploy", true));
        return PlayState.STOP;
    }

    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(
            new AnimationController<DiagonalTileEntity>(this, "controller", 0, this::predicate));
    }

    @Override
    public AnimationFactory getFactory() {
        return factory;
    }

    @Override
    public void setBoundingBox(ComplexBB bb) {
        boundingBox = bb;
    }
}
