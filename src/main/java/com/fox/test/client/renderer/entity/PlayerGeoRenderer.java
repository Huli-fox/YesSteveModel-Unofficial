package com.fox.test.client.renderer.entity;

import com.fox.test.client.model.entity.PlayerGeoModel;
import com.fox.test.entity.PlayerWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.processor.IBone;
import software.bernie.geckolib3.model.provider.data.EntityModelData;
import software.bernie.geckolib3.renderers.geo.IGeoRenderer;
import software.bernie.geckolib3.geo.render.built.GeoModel;
import software.bernie.geckolib3.core.util.Color;

import java.util.Collections;

public class PlayerGeoRenderer extends Render implements IGeoRenderer<PlayerWrapper> {
    private final PlayerGeoModel modelProvider;
    
    public PlayerGeoRenderer() {
        this.modelProvider = new PlayerGeoModel();
    }
    
    public void renderPlayer(EntityPlayer player, double x, double y, double z, float entityYaw, float partialTicks) {
        PlayerWrapper wrapper = new PlayerWrapper(player);
        
        // 更新玩家移动方向，使身体朝向视角方向
        wrapper.updateMovingDirection();
        
        GL11.glPushMatrix();
        // 使用与实体渲染器相同的定位方式，直接使用传入的坐标
        // 这样可以确保模型底部与玩家实体底部对齐
        GL11.glTranslated(x, y - player.yOffset, z);
        GL11.glScalef(1.0F, 1.0F, 1.0F);
        
        // 构建动画事件
        EntityModelData entityModelData = new EntityModelData();
        entityModelData.isSitting = false;
        entityModelData.isChild = player.isChild();
        
        // 计算插值后的身体旋转角度 (Interpolated Body Yaw)
        // 这会让身体的转向变得平滑，而不是瞬间转动
        float interpolatedBodyYaw = player.prevRenderYawOffset + (player.renderYawOffset - player.prevRenderYawOffset) * partialTicks;
        float headPitch = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * partialTicks;
        float f1 = player.prevRotationYawHead + (player.rotationYawHead - player.prevRotationYawHead) * partialTicks;
        float netHeadYaw = f1 - interpolatedBodyYaw;
        entityModelData.headPitch = -headPitch;
        entityModelData.netHeadYaw = -netHeadYaw;
        
        // 创建正确的AnimationEvent，包含肢体摆动参数
        float limbSwing = 0.0F;
        float limbSwingAmount = 0.0F;
        if (player.isEntityAlive()) {
            limbSwingAmount = player.prevLimbSwingAmount + (player.limbSwingAmount - player.prevLimbSwingAmount) * partialTicks;
            limbSwing = player.limbSwing - player.limbSwingAmount * (1.0F - partialTicks);
            if (player.isChild()) {
                limbSwing *= 3.0F;
            }
        }
        
        AnimationEvent<PlayerWrapper> predicate = new AnimationEvent<>(wrapper, limbSwing, limbSwingAmount, partialTicks, 
            !(limbSwingAmount > -0.15F && limbSwingAmount < 0.15F), Collections.singletonList(entityModelData));
        
        // 获取模型
        GeoModel model = this.modelProvider.getModel(this.modelProvider.getModelLocation(wrapper));
        
        // 应用动画 - 使用AnimationFactory的hashCode作为唯一标识符
        this.modelProvider.setLivingAnimations(wrapper, wrapper.getFactory().hashCode(), predicate);
        
        // 安全地绑定纹理
        ResourceLocation textureLocation = this.modelProvider.getTextureLocation(wrapper);
        if (textureLocation != null && Minecraft.getMinecraft().renderEngine != null) {
            Minecraft.getMinecraft().renderEngine.bindTexture(textureLocation);
        }
        
        // 渲染模型
        this.render(model, wrapper, partialTicks, 1.0F, 1.0F, 1.0F, 1.0F);
        
        GL11.glPopMatrix();
    }
    
    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        if (entity instanceof EntityPlayer) {
            PlayerWrapper wrapper = new PlayerWrapper((EntityPlayer) entity);
            ResourceLocation textureLocation = this.modelProvider.getTextureLocation(wrapper);
            // 如果纹理位置为空，则返回默认的紫黑纹理
            return textureLocation != null ? textureLocation : new ResourceLocation("missingno");
        }
        return new ResourceLocation("missingno");
    }
    
    // 实现IGeoRenderer接口的getTextureLocation方法
    @Override
    public ResourceLocation getTextureLocation(PlayerWrapper instance) {
        return this.modelProvider.getTextureLocation(instance);
    }
    
    // 实现抽象方法
    @Override
    public void doRender(Entity entity, double x, double y, double z, float entityYaw, float partialTicks) {
        if (entity instanceof EntityPlayer) {
            renderPlayer((EntityPlayer) entity, x, y, z, entityYaw, partialTicks);
        }
    }

    @Override
    public PlayerGeoModel getGeoModelProvider() {
        return this.modelProvider;
    }
}