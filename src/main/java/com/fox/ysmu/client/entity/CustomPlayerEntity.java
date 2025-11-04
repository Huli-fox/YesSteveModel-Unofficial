package com.fox.ysmu.client.entity;

import static com.fox.ysmu.util.ControllerUtils.*;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.fox.ysmu.client.ClientModelManager;
import com.fox.ysmu.client.animation.AnimationManager;
import com.fox.ysmu.client.animation.condition.ConditionArmor;
import com.fox.ysmu.client.model.CustomPlayerModel;

import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.builder.ILoopType;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;
import software.bernie.geckolib3.resource.GeckoLibCache;
import software.bernie.geckolib3.util.GeckoLibUtil;

public class CustomPlayerEntity implements IAnimatable {

    private final AnimationFactory factory = GeckoLibUtil.createFactory(this, true);
    private ResourceLocation mainModel = CustomPlayerModel.DEFAULT_MAIN_MODEL;
    private ResourceLocation texture = CustomPlayerModel.DEFAULT_TEXTURE;
    private String previewAnimation = "";
    private EntityPlayer player = null;

    @NotNull
    private static <P extends IAnimatable> PlayState playLoopAnimation(AnimationEvent<P> event, String animationName) {
        event.getController()
            .setAnimation(new AnimationBuilder().addAnimation(animationName, ILoopType.EDefaultLoopTypes.LOOP));
        return PlayState.CONTINUE;
    }

    /**
     * 越往后优先级越高
     */
    @Override

    @SuppressWarnings("all")
    public void registerControllers(AnimationData data) {
        AnimationManager manager = AnimationManager.getInstance();
        for (int i = 0; i < 8; i++) {
            String controllerName = String.format("pre_parallel_%d_controller", i);
            String animationName = String.format("pre_parallel%d", i);
            data.addAnimationController(
                new AnimationController<>(this, controllerName, 0, e -> manager.predicateParallel(e, animationName)));
        }
        data.addAnimationController(new AnimationController(this, MAIN_CONTROLLER, 2, manager::predicateMain));
        data.addAnimationController(
            new AnimationController(this, HOLD_OFFHAND_CONTROLLER, 0, manager::predicateOffhandHold));
        data.addAnimationController(
            new AnimationController(this, HOLD_MAINHAND_CONTROLLER, 0, manager::predicateMainhandHold));
        data.addAnimationController(new AnimationController(this, SWING_CONTROLLER, 2, manager::predicateSwing));
        data.addAnimationController(new AnimationController(this, USE_CONTROLLER, 2, manager::predicateUse));
        for (int i = 0; i < 8; i++) {
            String controllerName = String.format("parallel_%d_controller", i);
            String animationName = String.format("parallel%d", i);
            data.addAnimationController(
                new AnimationController<>(this, controllerName, 0, e -> manager.predicateParallel(e, animationName)));
        }
        // 为每个盔甲槽位注册控制器，使用1-4的索引值
        for (int slotIndex = 1; slotIndex <= 4; slotIndex++) {
            String controllerName = String.format("%s_controller", ConditionArmor.getSlotNameFromIndex(slotIndex));
            int finalSlotIndex = slotIndex;
            data.addAnimationController(
                new AnimationController(this, controllerName, 0, e -> manager.predicateArmor(e, finalSlotIndex)));
        }
        data.addAnimationController(new AnimationController(this, CAP_CONTROLLER, 2, manager::predicateCap));
    }

    public ResourceLocation getMainModel() {
        if (GeckoLibCache.getInstance()
            .getGeoModels()
            .containsKey(this.mainModel)) {
            return mainModel;
        }
        return CustomPlayerModel.DEFAULT_MAIN_MODEL;
    }

    public void setMainModel(ResourceLocation mainModel) {
        this.mainModel = mainModel;
    }

    public ResourceLocation getAnimation() {
        if (GeckoLibCache.getInstance()
            .getAnimations()
            .containsKey(this.mainModel)) {
            return mainModel;
        }
        return CustomPlayerModel.DEFAULT_MAIN_ANIMATION;
    }

    public float getHeightScale() {
        if (ClientModelManager.SCALE_INFO.containsKey(this.mainModel)) {
            return ClientModelManager.SCALE_INFO.get(this.mainModel)
                .left()
                .floatValue();
        }
        return 0.7f;
    }

    public float getWidthScale() {
        if (ClientModelManager.SCALE_INFO.containsKey(this.mainModel)) {
            return ClientModelManager.SCALE_INFO.get(this.mainModel)
                .right()
                .floatValue();
        }
        return 0.7f;
    }

    public EntityPlayer getPlayer() {
        return player;
    }

    public void setPlayer(EntityPlayer player) {
        this.player = player;
    }

    @Override

    public AnimationFactory getFactory() {
        return this.factory;
    }

    public ResourceLocation getTexture() {
        return texture;
    }

    public void setTexture(ResourceLocation texture) {
        this.texture = texture;
    }

    public String getPreviewAnimation() {
        return previewAnimation;
    }

    public void setPreviewAnimation(String previewAnimation) {
        this.previewAnimation = previewAnimation;
    }

    public void clearPreviewAnimation() {
        this.previewAnimation = "";
    }

    public boolean hasPreviewAnimation() {
        return StringUtils.isNoneBlank(this.previewAnimation);
    }

    public boolean hasPreviewAnimation(String previewAnimation) {
        return hasPreviewAnimation() && previewAnimation.equals(this.previewAnimation);
    }
}
