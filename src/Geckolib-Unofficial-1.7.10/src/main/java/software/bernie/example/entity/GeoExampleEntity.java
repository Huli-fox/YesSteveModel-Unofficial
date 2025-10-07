package software.bernie.example.entity; //The package our class is located in

//Imports of the classes used in this class description

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.IAnimationTickable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

//Our entity will be derived from the EntityCreature class, meaning that it will be a peaceful creature with some simple AI
//It will also implement IAnimatable and IAnimationTickable interfaces, which are required for something to have a geckolib animated model
public class GeoExampleEntity extends EntityCreature implements IAnimatable, IAnimationTickable {
    //We create a field for an animation factory - a special storage for all of the animation states for our entity
    private final AnimationFactory factory = new AnimationFactory(this);

    //The constructor of our class, requires a World instance and uses it for the call of super-constructor
    public GeoExampleEntity(World worldIn) {
        super(worldIn);
        this.ignoreFrustumCheck = true; //The entity will be rendered even if it's hitbox is not visible
        //Here we add an AI task for our entity to watch the closest player in the range of 8 blocks
        this.tasks.addTask(6, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
        this.setSize(5F, 5F); //the size of the entity hitbox
    }

    //This function is called a predicate. It is used to determine what animation should be played and whether it should play at all
    // based on the entity state, which is provided in the event argument
    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        //We set the controller to play the looping "animation.bat.fly" animation
        event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.bat.fly", true));
        return PlayState.CONTINUE; //And we say that the animation should be played
    }

    //This function is used to register the animation controllers, which will play animations determined by the predicate given
    @Override
    public void registerControllers(AnimationData data) {
        //Here we register a controller with name "controller", with predicate referenced as this::predicate
        //And we set transition length to 50 ticks, meaning that when one animation ends, the model will transition
        //To the beginning of the next animation smoothly during those 50 ticks. It can be set to 0 for immediate switch between animations
        data.addAnimationController(new AnimationController<>(this, "controller", 50, this::predicate));
    }

    //Here we add a getter-function so that geckolib could get the reference to the animation factory
    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }

    //This function should return the value of a timer which will increase every tick - the ticksExisted field fits very well for that
    @Override
    public int tickTimer() {
        return ticksExisted;
    }
}
