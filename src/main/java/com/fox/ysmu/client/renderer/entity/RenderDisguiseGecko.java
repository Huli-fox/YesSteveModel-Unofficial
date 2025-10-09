package com.fox.ysmu.client.renderer.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity; // 新增导入
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation; // 新增导入
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;
import com.fox.ysmu.client.model.entity.ModelDisguiseGecko;
import com.fox.ysmu.entity.EntityDisguiseGecko;
import com.fox.ysmu.TransformationEventHandler;


public class RenderDisguiseGecko extends GeoEntityRenderer<EntityDisguiseGecko> {

    public RenderDisguiseGecko() {
        super(new ModelDisguiseGecko());
    }

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer localPlayer = mc.thePlayer;

        // 如果玩家不存在，或游戏世界不存在，则执行默认渲染
        if (localPlayer == null || mc.theWorld == null) {
            super.doRender(entity, x, y, z, yaw, partialTicks);
            return;
        }

        // 检查本地玩家是否正在变身
        Integer disguiseId = TransformationEventHandler.transformationMap.get(localPlayer.getUniqueID());

        // 如果满足以下所有条件，就直接返回，不进行任何渲染：
        // 1. 玩家正在变身 (disguiseId != null)
        // 2. 当前要渲染的实体就是这个变身模型 (entity.getEntityId() == disguiseId)
        // 3. 玩家正处于第一人称视角 (mc.gameSettings.thirdPersonView == 0)
        if (disguiseId != null && entity.getEntityId() == disguiseId && mc.gameSettings.thirdPersonView == 0) {
            return; // <<--- 在这里直接拦截，阻止渲染！
        }

        // 如果不满足上述条件，则执行正常的Geckolib渲染
        super.doRender(entity, x, y, z, yaw, partialTicks);
    }

    /**
     * 【修正】覆盖基类 'Render' 的方法，使用正确的字段 'modelProvider'。
     * 访问修饰符应为 protected 以匹配父类。
     */
    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        // 调用我们模型提供者中的 getTextureLocation 方法
        return this.modelProvider.getTextureLocation((EntityDisguiseGecko) entity);
    }
}