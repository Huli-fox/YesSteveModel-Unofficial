package software.bernie.example.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.ai.EntityAIControlledByPlayer;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import javax.annotation.Nullable;

public class BikeEntity extends EntityAnimal implements IAnimatable {
    private AnimationFactory factory = new AnimationFactory(this);
    private final EntityAIControlledByPlayer aiControlledByPlayer;

    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.bike.idle", true));
        return PlayState.CONTINUE;
    }

    public BikeEntity(World worldIn) {
        super(worldIn);
        this.ignoreFrustumCheck = true;
        this.tasks.addTask(2, this.aiControlledByPlayer = new EntityAIControlledByPlayer(this, 0.3F));
        this.setSize(0.5F, 0.6F);
    }

    @Override
    public boolean interact(EntityPlayer player) {
        if (this.riddenByEntity == null) {
            player.mountEntity(this);
            return super.interact(player);
        }
        return super.interact(player);
    }

//	@Override
//	public void travel(float strafe, float vertical, float forward) {
//		if (this.isEntityAlive()) {
//			if (this.riddenByEntity != null) {
//				EntityLivingBase livingentity = (EntityLivingBase) this.getControllingPassenger();
//				this.rotationYaw = livingentity.rotationYaw;
//				this.prevRotationYaw = this.rotationYaw;
//				this.rotationPitch = livingentity.rotationPitch * 0.5F;
//				this.setRotation(this.rotationYaw, this.rotationPitch);
//				this.renderYawOffset = this.rotationYaw;
//				this.rotationYawHead = this.renderYawOffset;
//				float f = livingentity.moveStrafing * 0.5F;
//				float f1 = livingentity.moveForward;
//				if (f1 <= 0.0F) {
//					f1 *= 0.25F;
//				}
//
//				this.setAIMoveSpeed(0.3F);
//				super.travel(f, vertical, f1);
//			}
//		}
//	}

    @Nullable
    public Entity getControllingPassenger() {
        return this.ridingEntity;
    }

    @Override
    public boolean canBeSteered() {
        return true;
    }

    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<BikeEntity>(this, "controller", 0, this::predicate));
    }

    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }

    @Nullable
    @Override
    public EntityAgeable createChild(EntityAgeable ageable) {
        return null;
    }
}
