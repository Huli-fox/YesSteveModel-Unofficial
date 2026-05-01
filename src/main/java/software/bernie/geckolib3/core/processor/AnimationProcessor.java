package software.bernie.geckolib3.core.processor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Maps;

import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.IAnimatableModel;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.keyframe.AnimationPoint;
import software.bernie.geckolib3.core.keyframe.BoneAnimationQueue;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.molang.MolangParser;
import software.bernie.geckolib3.core.snapshot.BoneSnapshot;
import software.bernie.geckolib3.core.snapshot.DirtyTracker;
import software.bernie.geckolib3.core.util.MathUtil;
import software.bernie.geckolib3.model.provider.data.EntityModelData;

public class AnimationProcessor<T extends IAnimatable> {

    public boolean reloadAnimations = false;
    private List<IBone> modelRendererList = new ArrayList();
    private Map<Integer, AnimationRenderState> animatedEntities = new HashMap<>();
    private final IAnimatableModel animatedModel;

    public AnimationProcessor(IAnimatableModel animatedModel) {
        this.animatedModel = animatedModel;
    }

    public void tickAnimation(IAnimatable entity, Integer uniqueID, double seekTime, AnimationEvent event,
        MolangParser parser, boolean crashWhenCantFindBone) {
        AnimationRenderState renderState = AnimationRenderState.from(seekTime, event);
        if (renderState.equals(animatedEntities.get(uniqueID))) {
            return;
        }
        animatedEntities.put(uniqueID, renderState);

        // Each animation has it's own collection of animations (called the
        // EntityAnimationManager), which allows for multiple independent animations
        AnimationData manager = entity.getFactory()
            .getOrCreateAnimationData(uniqueID);
        // Keeps track of which bones have had animations applied to them, and
        // eventually sets the ones that don't have an animation to their default values
        HashMap<String, DirtyTracker> modelTracker = createNewDirtyTracker();

        // Store the current value of each bone rotation/position/scale
        updateBoneSnapshots(manager.getBoneSnapshotCollection());
        HashMap<String, Pair<IBone, BoneSnapshot>> boneSnapshots = manager.getBoneSnapshotCollection();
        HashMap<String, PointData> pointDataGroup = Maps.newHashMap();
        for (AnimationController<T> controller : manager.getAnimationControllers()
            .values()) {
            if (reloadAnimations) {
                controller.markNeedsReload();
                controller.getBoneAnimationQueues()
                    .clear();
            }

            controller.isJustStarting = manager.isFirstTick;

            // Set current controller to animation test event
            event.setController(controller);

            // Process animations and add new values to the point queues
            controller.process(seekTime, event, modelRendererList, boneSnapshots, parser, crashWhenCantFindBone);

            // Loop through every single bone and lerp each property
            for (BoneAnimationQueue boneAnimation : controller.getBoneAnimationQueues()
                .values()) {
                IBone bone = boneAnimation.bone;
                BoneSnapshot snapshot = boneSnapshots.get(bone.getName())
                    .getRight();
                BoneSnapshot initialSnapshot = bone.getInitialSnapshot();
                pointDataGroup.putIfAbsent(bone.getName(), new PointData());
                PointData pointData = pointDataGroup.get(bone.getName());

                AnimationPoint rXPoint = boneAnimation.rotationXQueue.poll();
                AnimationPoint rYPoint = boneAnimation.rotationYQueue.poll();
                AnimationPoint rZPoint = boneAnimation.rotationZQueue.poll();

                AnimationPoint pXPoint = boneAnimation.positionXQueue.poll();
                AnimationPoint pYPoint = boneAnimation.positionYQueue.poll();
                AnimationPoint pZPoint = boneAnimation.positionZQueue.poll();

                AnimationPoint sXPoint = boneAnimation.scaleXQueue.poll();
                AnimationPoint sYPoint = boneAnimation.scaleYQueue.poll();
                AnimationPoint sZPoint = boneAnimation.scaleZQueue.poll();

                // If there's any rotation points for this bone
                DirtyTracker dirtyTracker = modelTracker.get(bone.getName());
                if (dirtyTracker == null) {
                    continue;
                }
                if (rXPoint != null && rYPoint != null && rZPoint != null) {
                    float valueX = MathUtil.lerpValues(rXPoint, controller.easingType, controller.customEasingMethod);
                    float valueY = MathUtil.lerpValues(rYPoint, controller.easingType, controller.customEasingMethod);
                    float valueZ = MathUtil.lerpValues(rZPoint, controller.easingType, controller.customEasingMethod);
                    pointData.rotationValueX += valueX;
                    pointData.rotationValueY += valueY;
                    pointData.rotationValueZ += valueZ;
                    if (controller.getName()
                        .startsWith("parallel_")) {
                        bone.setRotationX(pointData.rotationValueX + initialSnapshot.rotationValueX);
                        bone.setRotationY(pointData.rotationValueY + initialSnapshot.rotationValueY);
                        bone.setRotationZ(pointData.rotationValueZ + initialSnapshot.rotationValueZ);
                    } else {
                        bone.setRotationX(valueX + initialSnapshot.rotationValueX);
                        bone.setRotationY(valueY + initialSnapshot.rotationValueY);
                        bone.setRotationZ(valueZ + initialSnapshot.rotationValueZ);
                    }
                    snapshot.rotationValueX = bone.getRotationX();
                    snapshot.rotationValueY = bone.getRotationY();
                    snapshot.rotationValueZ = bone.getRotationZ();
                    snapshot.isCurrentlyRunningRotationAnimation = true;
                    dirtyTracker.hasRotationChanged = true;
                }

                // If there's any position points for this bone
                if (pXPoint != null && pYPoint != null && pZPoint != null) {
                    bone.setPositionX(
                        MathUtil.lerpValues(pXPoint, controller.easingType, controller.customEasingMethod));
                    bone.setPositionY(
                        MathUtil.lerpValues(pYPoint, controller.easingType, controller.customEasingMethod));
                    bone.setPositionZ(
                        MathUtil.lerpValues(pZPoint, controller.easingType, controller.customEasingMethod));
                    snapshot.positionOffsetX = bone.getPositionX();
                    snapshot.positionOffsetY = bone.getPositionY();
                    snapshot.positionOffsetZ = bone.getPositionZ();
                    snapshot.isCurrentlyRunningPositionAnimation = true;

                    dirtyTracker.hasPositionChanged = true;
                }

                // If there's any scale points for this bone
                if (sXPoint != null && sYPoint != null && sZPoint != null) {
                    bone.setScaleX(MathUtil.lerpValues(sXPoint, controller.easingType, controller.customEasingMethod));
                    bone.setScaleY(MathUtil.lerpValues(sYPoint, controller.easingType, controller.customEasingMethod));
                    bone.setScaleZ(MathUtil.lerpValues(sZPoint, controller.easingType, controller.customEasingMethod));
                    snapshot.scaleValueX = bone.getScaleX();
                    snapshot.scaleValueY = bone.getScaleY();
                    snapshot.scaleValueZ = bone.getScaleZ();
                    snapshot.isCurrentlyRunningScaleAnimation = true;

                    dirtyTracker.hasScaleChanged = true;
                }
            }
        }

        this.reloadAnimations = false;

        double resetTickLength = manager.getResetSpeed();
        for (Map.Entry<String, DirtyTracker> tracker : modelTracker.entrySet()) {
            IBone model = tracker.getValue().model;
            BoneSnapshot initialSnapshot = model.getInitialSnapshot();
            BoneSnapshot saveSnapshot = boneSnapshots.get(tracker.getKey())
                .getRight();
            if (saveSnapshot == null) {
                if (crashWhenCantFindBone) {
                    throw new RuntimeException(
                        "Could not find save snapshot for bone: " + tracker.getValue().model.getName()
                            + ". Please don't add bones that are used in an animation at runtime.");
                } else {
                    continue;
                }
            }

            if (!tracker.getValue().hasRotationChanged) {
                if (saveSnapshot.isCurrentlyRunningRotationAnimation) {
                    saveSnapshot.mostRecentResetRotationTick = 0; // TODO 原为(float) seekTime，旋转问题相关
                    saveSnapshot.isCurrentlyRunningRotationAnimation = false;
                }

                double percentageReset = Math
                    .min((seekTime - saveSnapshot.mostRecentResetRotationTick) / resetTickLength, 1);

                model.setRotationX(
                    MathUtil.lerpValues(percentageReset, saveSnapshot.rotationValueX, initialSnapshot.rotationValueX));
                model.setRotationY(
                    MathUtil.lerpValues(percentageReset, saveSnapshot.rotationValueY, initialSnapshot.rotationValueY));
                model.setRotationZ(
                    MathUtil.lerpValues(percentageReset, saveSnapshot.rotationValueZ, initialSnapshot.rotationValueZ));

                if (percentageReset >= 1) {
                    saveSnapshot.rotationValueX = model.getRotationX();
                    saveSnapshot.rotationValueY = model.getRotationY();
                    saveSnapshot.rotationValueZ = model.getRotationZ();
                }
            }
            if (!tracker.getValue().hasPositionChanged) {
                if (saveSnapshot.isCurrentlyRunningPositionAnimation) {
                    saveSnapshot.mostRecentResetPositionTick = (float) seekTime;
                    saveSnapshot.isCurrentlyRunningPositionAnimation = false;
                }

                double percentageReset = Math
                    .min((seekTime - saveSnapshot.mostRecentResetPositionTick) / resetTickLength, 1);

                model.setPositionX(
                    MathUtil
                        .lerpValues(percentageReset, saveSnapshot.positionOffsetX, initialSnapshot.positionOffsetX));
                model.setPositionY(
                    MathUtil
                        .lerpValues(percentageReset, saveSnapshot.positionOffsetY, initialSnapshot.positionOffsetY));
                model.setPositionZ(
                    MathUtil
                        .lerpValues(percentageReset, saveSnapshot.positionOffsetZ, initialSnapshot.positionOffsetZ));

                if (percentageReset >= 1) {
                    saveSnapshot.positionOffsetX = model.getPositionX();
                    saveSnapshot.positionOffsetY = model.getPositionY();
                    saveSnapshot.positionOffsetZ = model.getPositionZ();
                }
            }
            if (!tracker.getValue().hasScaleChanged) {
                if (saveSnapshot.isCurrentlyRunningScaleAnimation) {
                    saveSnapshot.mostRecentResetScaleTick = (float) seekTime;
                    saveSnapshot.isCurrentlyRunningScaleAnimation = false;
                }

                double percentageReset = Math
                    .min((seekTime - saveSnapshot.mostRecentResetScaleTick) / resetTickLength, 1);

                model.setScaleX(
                    MathUtil.lerpValues(percentageReset, saveSnapshot.scaleValueX, initialSnapshot.scaleValueX));
                model.setScaleY(
                    MathUtil.lerpValues(percentageReset, saveSnapshot.scaleValueY, initialSnapshot.scaleValueY));
                model.setScaleZ(
                    MathUtil.lerpValues(percentageReset, saveSnapshot.scaleValueZ, initialSnapshot.scaleValueZ));

                if (percentageReset >= 1) {
                    saveSnapshot.scaleValueX = model.getScaleX();
                    saveSnapshot.scaleValueY = model.getScaleY();
                    saveSnapshot.scaleValueZ = model.getScaleZ();
                }
            }
        }
        manager.isFirstTick = false;
    }

    private HashMap<String, DirtyTracker> createNewDirtyTracker() {
        HashMap<String, DirtyTracker> tracker = new HashMap<>();
        for (IBone bone : modelRendererList) {
            tracker.put(bone.getName(), new DirtyTracker(false, false, false, bone));
        }
        return tracker;
    }

    private void updateBoneSnapshots(HashMap<String, Pair<IBone, BoneSnapshot>> boneSnapshotCollection) {
        for (IBone bone : modelRendererList) {
            if (!boneSnapshotCollection.containsKey(bone.getName())) {
                boneSnapshotCollection.put(bone.getName(), Pair.of(bone, new BoneSnapshot(bone.getInitialSnapshot())));
            }
        }
    }

    /**
     * Gets a bone by name.
     *
     * @param boneName The bone name
     * @return the bone
     */
    public IBone getBone(String boneName) {
        return modelRendererList.stream()
            .filter(
                x -> x.getName()
                    .equals(boneName))
            .findFirst()
            .orElse(null);
    }

    /**
     * Register model renderer. Each AnimatedModelRenderer (group in blockbench)
     * NEEDS to be registered via this method.
     *
     * @param modelRenderer The model renderer
     */
    public void registerModelRenderer(IBone modelRenderer) {
        modelRenderer.saveInitialSnapshot();
        modelRendererList.add(modelRenderer);
    }

    public void clearModelRendererList() {
        this.modelRendererList.clear();
    }

    public List<IBone> getModelRendererList() {
        return modelRendererList;
    }

    public void preAnimationSetup(IAnimatable animatable, double seekTime) {
        this.animatedModel.setMolangQueries(animatable, seekTime);
    }

    private static final class AnimationRenderState {
        private final double seekTime;
        private final float limbSwing;
        private final float limbSwingAmount;
        private final float partialTick;
        private final boolean moving;
        private final boolean hasEntityModelData;
        private final int entityModelDataIdentity;
        private final boolean sitting;
        private final boolean child;
        private final float netHeadYaw;
        private final float headPitch;

        private AnimationRenderState(double seekTime, AnimationEvent event, EntityModelData entityModelData) {
            this.seekTime = seekTime;
            this.limbSwing = event.getLimbSwing();
            this.limbSwingAmount = event.getLimbSwingAmount();
            this.partialTick = event.getPartialTick();
            this.moving = event.isMoving();
            this.hasEntityModelData = entityModelData != null;
            // EntityModelData is recreated per render call; its identity separates GUI and world poses at the same tick.
            this.entityModelDataIdentity = entityModelData == null ? 0 : System.identityHashCode(entityModelData);
            this.sitting = entityModelData != null && entityModelData.isSitting;
            this.child = entityModelData != null && entityModelData.isChild;
            this.netHeadYaw = entityModelData == null ? 0.0F : entityModelData.netHeadYaw;
            this.headPitch = entityModelData == null ? 0.0F : entityModelData.headPitch;
        }

        private static AnimationRenderState from(double seekTime, AnimationEvent event) {
            return new AnimationRenderState(seekTime, event, getEntityModelData(event));
        }

        private static EntityModelData getEntityModelData(AnimationEvent event) {
            for (Object data : event.getExtraData()) {
                if (data instanceof EntityModelData) {
                    return (EntityModelData) data;
                }
            }
            return null;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof AnimationRenderState)) {
                return false;
            }
            AnimationRenderState other = (AnimationRenderState) obj;
            return Double.doubleToLongBits(this.seekTime) == Double.doubleToLongBits(other.seekTime)
                && Float.floatToIntBits(this.limbSwing) == Float.floatToIntBits(other.limbSwing)
                && Float.floatToIntBits(this.limbSwingAmount) == Float.floatToIntBits(other.limbSwingAmount)
                && Float.floatToIntBits(this.partialTick) == Float.floatToIntBits(other.partialTick)
                && this.moving == other.moving
                && this.hasEntityModelData == other.hasEntityModelData
                && this.entityModelDataIdentity == other.entityModelDataIdentity
                && this.sitting == other.sitting
                && this.child == other.child
                && Float.floatToIntBits(this.netHeadYaw) == Float.floatToIntBits(other.netHeadYaw)
                && Float.floatToIntBits(this.headPitch) == Float.floatToIntBits(other.headPitch);
        }

        @Override
        public int hashCode() {
            int result = (int) (Double.doubleToLongBits(this.seekTime) ^ (Double.doubleToLongBits(this.seekTime) >>> 32));
            result = 31 * result + Float.floatToIntBits(this.limbSwing);
            result = 31 * result + Float.floatToIntBits(this.limbSwingAmount);
            result = 31 * result + Float.floatToIntBits(this.partialTick);
            result = 31 * result + (this.moving ? 1 : 0);
            result = 31 * result + (this.hasEntityModelData ? 1 : 0);
            result = 31 * result + this.entityModelDataIdentity;
            result = 31 * result + (this.sitting ? 1 : 0);
            result = 31 * result + (this.child ? 1 : 0);
            result = 31 * result + Float.floatToIntBits(this.netHeadYaw);
            result = 31 * result + Float.floatToIntBits(this.headPitch);
            return result;
        }
    }
}
