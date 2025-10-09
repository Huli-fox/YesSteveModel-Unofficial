package com.fox.ysmu.client.renderer.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.Entity; // 新增导入
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation; // 新增导入
import software.bernie.geckolib3.core.processor.IBone;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;
import com.fox.ysmu.client.model.entity.ModelDisguiseGecko;
import com.fox.ysmu.entity.EntityDisguiseGecko;
import com.fox.ysmu.TransformationEventHandler;
import org.lwjgl.opengl.GL11;


public class RenderDisguiseGecko extends GeoEntityRenderer<EntityDisguiseGecko> {

    public RenderDisguiseGecko() {
        super(new ModelDisguiseGecko());
        this.shadowSize = 0.5F;
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

        EntityDisguiseGecko disguise = (EntityDisguiseGecko) entity;
        ItemStack itemstack = disguise.getHeldItem();

        if (itemstack != null) {
            // 从模型实例中直接获取骨骼
            // 我们需要将 getGeoModelProvider() 的结果转换为我们自己的模型类型
            ModelDisguiseGecko model = (ModelDisguiseGecko) this.getGeoModelProvider();
            IBone rightHand = model.getAnimationProcessor().getBone("RightArm"); // 修正：正确的调用链

            if (rightHand != null) {
                // 执行物品渲染。我们需要手动处理矩阵变换，因为我们不再处于renderLate的上下文中
                GL11.glPushMatrix();

                // 将坐标系移动到实体的位置
                GL11.glTranslated(x, y, z);

                // 【重要】手动应用实体的旋转，让物品和身体一起转动
                // 这部分代码模拟了渲染引擎的部分工作
                GL11.glRotatef(-disguise.renderYawOffset, 0.0F, 1.0F, 0.0F);

                renderItemInHand(disguise, itemstack, rightHand);
                GL11.glPopMatrix();
            }
        }
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


    /**
     * 一个辅助方法，用于在指定的骨骼上渲染物品
     */
    private void renderItemInHand(EntityDisguiseGecko entity, ItemStack itemstack, IBone handBone) {
        GL11.glPushMatrix(); // 保存当前矩阵状态

        // 1. 移动到骨骼的位置
        GL11.glTranslated(handBone.getPositionX() / 16.0F, handBone.getPositionY() / 16.0F, handBone.getPositionZ() / 16.0F);

        // 2. 应用骨骼的旋转 (注意从弧度转换为角度)
        GL11.glRotatef(handBone.getRotationZ() * (180F / (float)Math.PI), 0.0F, 0.0F, 1.0F);
        GL11.glRotatef(handBone.getRotationY() * (180F / (float)Math.PI), 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(handBone.getRotationX() * (180F / (float)Math.PI), 1.0F, 0.0F, 0.0F);

        // 3. 需要进行一些微调，让物品正确地握在手中
        GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F); // 将物品竖起来
        GL11.glTranslatef(0.0F, 0.125F, -0.3F); // 位置微调 (X, Y, Z)
        GL11.glScalef(0.8F, 0.8F, 0.8F);       // 缩放微调

        // 4. 调用Minecraft的物品渲染器
        this.renderManager.itemRenderer.renderItem(entity, itemstack, 0);

        // 如果是方块，渲染它的3D形态
        if (itemstack.getItem().requiresMultipleRenderPasses()) {
            this.renderManager.itemRenderer.renderItem(entity, itemstack, 1);
        }

        GL11.glPopMatrix(); // 恢复矩阵状态
    }
}