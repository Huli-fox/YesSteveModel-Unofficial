package software.bernie.geckolib3.geo.raw.pojo;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ModelProperties implements Serializable {

    private static final long serialVersionUID = 42L;
    private Boolean animationArmsDown;
    private Boolean animationArmsOutFront;
    private Boolean animationDontShowArmor;
    private Boolean animationInvertedCrouch;
    private Boolean animationNoHeadBob;
    private Boolean animationSingleArmAnimation;
    private Boolean animationSingleLegAnimation;
    private Boolean animationStationaryLegs;
    private Boolean animationStatueOfLibertyArms;
    private Boolean animationUpsideDown;
    private String identifier;
    private Boolean preserveModelPose;
    private Double textureHeight;
    private Double textureWidth;
    private Double visibleBoundsHeight;
    private double[] visibleBoundsOffset;
    private Double visibleBoundsWidth;
    private Double heightScale = 0.7D;
    private Double widthScale = 0.7D;
    private ExtraInfo extraInfo = null;

    @JsonProperty("animationArmsDown")
    public Boolean getAnimationArmsDown() {
        return animationArmsDown;
    }

    @JsonProperty("animationArmsDown")
    public void setAnimationArmsDown(Boolean value) {
        this.animationArmsDown = value;
    }

    @JsonProperty("animationArmsOutFront")
    public Boolean getAnimationArmsOutFront() {
        return animationArmsOutFront;
    }

    @JsonProperty("animationArmsOutFront")
    public void setAnimationArmsOutFront(Boolean value) {
        this.animationArmsOutFront = value;
    }

    @JsonProperty("animationDontShowArmor")
    public Boolean getAnimationDontShowArmor() {
        return animationDontShowArmor;
    }

    @JsonProperty("animationDontShowArmor")
    public void setAnimationDontShowArmor(Boolean value) {
        this.animationDontShowArmor = value;
    }

    @JsonProperty("animationInvertedCrouch")
    public Boolean getAnimationInvertedCrouch() {
        return animationInvertedCrouch;
    }

    @JsonProperty("animationInvertedCrouch")
    public void setAnimationInvertedCrouch(Boolean value) {
        this.animationInvertedCrouch = value;
    }

    @JsonProperty("animationNoHeadBob")
    public Boolean getAnimationNoHeadBob() {
        return animationNoHeadBob;
    }

    @JsonProperty("animationNoHeadBob")
    public void setAnimationNoHeadBob(Boolean value) {
        this.animationNoHeadBob = value;
    }

    @JsonProperty("animationSingleArmAnimation")
    public Boolean getAnimationSingleArmAnimation() {
        return animationSingleArmAnimation;
    }

    @JsonProperty("animationSingleArmAnimation")
    public void setAnimationSingleArmAnimation(Boolean value) {
        this.animationSingleArmAnimation = value;
    }

    @JsonProperty("animationSingleLegAnimation")
    public Boolean getAnimationSingleLegAnimation() {
        return animationSingleLegAnimation;
    }

    @JsonProperty("animationSingleLegAnimation")
    public void setAnimationSingleLegAnimation(Boolean value) {
        this.animationSingleLegAnimation = value;
    }

    @JsonProperty("animationStationaryLegs")
    public Boolean getAnimationStationaryLegs() {
        return animationStationaryLegs;
    }

    @JsonProperty("animationStationaryLegs")
    public void setAnimationStationaryLegs(Boolean value) {
        this.animationStationaryLegs = value;
    }

    @JsonProperty("animationStatueOfLibertyArms")
    public Boolean getAnimationStatueOfLibertyArms() {
        return animationStatueOfLibertyArms;
    }

    @JsonProperty("animationStatueOfLibertyArms")
    public void setAnimationStatueOfLibertyArms(Boolean value) {
        this.animationStatueOfLibertyArms = value;
    }

    @JsonProperty("animationUpsideDown")
    public Boolean getAnimationUpsideDown() {
        return animationUpsideDown;
    }

    @JsonProperty("animationUpsideDown")
    public void setAnimationUpsideDown(Boolean value) {
        this.animationUpsideDown = value;
    }

    @JsonProperty("identifier")
    public String getIdentifier() {
        return identifier;
    }

    @JsonProperty("identifier")
    public void setIdentifier(String value) {
        this.identifier = value;
    }

    @JsonProperty("preserve_model_pose")
    public Boolean getPreserveModelPose() {
        return preserveModelPose;
    }

    @JsonProperty("preserve_model_pose")
    public void setPreserveModelPose(Boolean value) {
        this.preserveModelPose = value;
    }

    /**
     * Assumed height in texels of the texture that will be bound to this geometry.
     */
    @JsonProperty("texture_height")
    public Double getTextureHeight() {
        return textureHeight;
    }

    @JsonProperty("texture_height")
    public void setTextureHeight(Double value) {
        this.textureHeight = value;
    }

    /**
     * Assumed width in texels of the texture that will be bound to this geometry.
     */
    @JsonProperty("texture_width")
    public Double getTextureWidth() {
        return textureWidth;
    }

    @JsonProperty("texture_width")
    public void setTextureWidth(Double value) {
        this.textureWidth = value;
    }

    /**
     * Height of the visible bounding box (in model space units).
     */
    @JsonProperty("visible_bounds_height")
    public Double getVisibleBoundsHeight() {
        return visibleBoundsHeight;
    }

    @JsonProperty("visible_bounds_height")
    public void setVisibleBoundsHeight(Double value) {
        this.visibleBoundsHeight = value;
    }

    /**
     * Offset of the visibility bounding box from the entity location point (in
     * model space units).
     */
    @JsonProperty("visible_bounds_offset")
    public double[] getVisibleBoundsOffset() {
        return visibleBoundsOffset;
    }

    @JsonProperty("visible_bounds_offset")
    public void setVisibleBoundsOffset(double[] value) {
        this.visibleBoundsOffset = value;
    }

    /**
     * Width of the visibility bounding box (in model space units).
     */
    @JsonProperty("visible_bounds_width")
    public Double getVisibleBoundsWidth() {
        return visibleBoundsWidth;
    }

    @JsonProperty("visible_bounds_width")
    public void setVisibleBoundsWidth(Double value) {
        this.visibleBoundsWidth = value;
    }

    @JsonProperty("ysm_height_scale")
    public Double getHeightScale() {
        return heightScale;
    }

    @JsonProperty("ysm_height_scale")
    public void setHeightScale(Double heightScale) {
        this.heightScale = heightScale;
    }

    @JsonProperty("ysm_width_scale")
    public Double getWidthScale() {
        return widthScale;
    }

    @JsonProperty("ysm_width_scale")
    public void setWidthScale(Double widthScale) {
        this.widthScale = widthScale;
    }

    @JsonProperty("ysm_extra_info")
    public ExtraInfo getExtraInfo() {
        return extraInfo;
    }

    @JsonProperty("ysm_extra_info")
    public void setExtraInfo(ExtraInfo extraInfo) {
        this.extraInfo = extraInfo;
    }
}
