package com.fox.ysmu;

import com.fox.ysmu.entity.EntityDisguiseGecko;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TransformationEventHandler {

    // 存储玩家UUID -> 伪装实体的 EntityID
    public static final Map<UUID, Integer> transformationMap = new ConcurrentHashMap<>();

    // 辅助方法：通过ID获取实体（在特定世界中）
    private Entity getEntityById(int id, World world) {
        if (world == null) return null;
        return world.getEntityByID(id);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            MinecraftServer server = MinecraftServer.getServer();
            if (server == null) return;

            for (Map.Entry<UUID, Integer> entry : transformationMap.entrySet()) {
                UUID playerUUID = entry.getKey();
                Integer entityId = entry.getValue();

                // 找到对应的玩家和实体
                EntityPlayer player = getPlayerByUUID(playerUUID);
                if (player != null) {
                    Entity disguise = getEntityById(entityId, player.worldObj);

                    if (player.isRiding()) {
                        // 如果玩家在骑乘，但伪装模型没有，就让它也骑上去
                        if (disguise.ridingEntity == null || disguise.ridingEntity.getEntityId() != player.ridingEntity.getEntityId()) {
                            disguise.mountEntity(player.ridingEntity);
                        }
                    } else {
                        // 如果玩家没骑，但伪装模型还骑着，就让它下来
                        if (disguise.ridingEntity != null) {
                            disguise.mountEntity(null);
                        }
                    }

                    if (disguise instanceof EntityLivingBase) {
                        // 同步状态
                        syncEntityStateFromServer(player, (EntityLivingBase) disguise);
                    } else if (disguise != null) {
                        // 如果不是LivingBase，只同步基础位置信息
                        disguise.setPositionAndRotation(player.posX, player.posY, player.posZ, player.rotationYaw, player.rotationPitch);
                    }
                }
            }
        }
    }

    /**
     * 服务器端的权威同步方法
     * @param sourcePlayer 源玩家
     * @param targetDisguise 目标伪装实体
     */
    public static void syncEntityStateFromServer(EntityPlayer sourcePlayer, EntityLivingBase targetDisguise) {
        if (!sourcePlayer.isRiding()) {
            double correctedY = sourcePlayer.posY - sourcePlayer.yOffset - targetDisguise.yOffset;
            targetDisguise.setPositionAndRotation(sourcePlayer.posX, correctedY, sourcePlayer.posZ, sourcePlayer.rotationYaw, sourcePlayer.rotationPitch);
            targetDisguise.renderYawOffset = sourcePlayer.renderYawOffset;
            targetDisguise.motionX = sourcePlayer.motionX;
            targetDisguise.motionY = sourcePlayer.motionY;
            targetDisguise.motionZ = sourcePlayer.motionZ;
        }
        // 同步头部转动
        targetDisguise.rotationYawHead = sourcePlayer.rotationYawHead;
        targetDisguise.renderYawOffset = sourcePlayer.renderYawOffset;

        // 3. 动画状态 (这些是让实体"活"起来的关键)
        targetDisguise.limbSwing = sourcePlayer.limbSwing;
        targetDisguise.limbSwingAmount = sourcePlayer.limbSwingAmount;
        targetDisguise.prevLimbSwingAmount = sourcePlayer.prevLimbSwingAmount;
        targetDisguise.swingProgress = sourcePlayer.swingProgress;
        targetDisguise.isSwingInProgress = sourcePlayer.isSwingInProgress;

        // 4. 其他状态
        targetDisguise.setSneaking(sourcePlayer.isSneaking());
        targetDisguise.setSprinting(sourcePlayer.isSprinting());
        targetDisguise.onGround = sourcePlayer.onGround;
        if (targetDisguise instanceof EntityDisguiseGecko) {
            EntityDisguiseGecko geckoDisguise = (EntityDisguiseGecko) targetDisguise;
            // 1. 同步飞行状态
            geckoDisguise.setFlying(sourcePlayer.capabilities.isFlying);
            // 2. 同步睡觉状态
            geckoDisguise.setSleeping(sourcePlayer.isPlayerSleeping());
        }
        if (sourcePlayer.isRiding()) {
            // 简单地同步ID可能不足以让客户端正确渲染，但这是服务器逻辑的基础
            if (targetDisguise.ridingEntity == null || targetDisguise.ridingEntity.getEntityId() != sourcePlayer.ridingEntity.getEntityId()) {
                // 注意：直接设置ridingEntity可能很复杂，更好的方式是让实体自己寻找并骑乘
                // 但对于纯视觉模型，我们主要同步状态
                targetDisguise.ridingEntity = sourcePlayer.ridingEntity;
            }
        } else {
            targetDisguise.ridingEntity = null;
        }
        targetDisguise.isOnLadder();
    }

    // (此处省略 getPlayerByUUID 的实现，请参考上一问的代码)
    private EntityPlayer getPlayerByUUID(UUID uuid) {
        if (uuid == null) return null;
        MinecraftServer server = MinecraftServer.getServer();
        if (server != null) {
            // 遍历所有玩家
            for (Object playerObj : server.getConfigurationManager().playerEntityList) {
                EntityPlayer player = (EntityPlayer) playerObj;
                if (player.getUniqueID().equals(uuid)) {
                    return player;
                }
            }
        }
        return null;
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onPlayerRender(RenderPlayerEvent.Pre event) {
        EntityPlayer player = event.entityPlayer;
        // 如果这个玩家在变身列表中，就取消渲染他
        if (transformationMap.containsKey(player.getUniqueID())) {
            event.setCanceled(true);
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer localPlayer = mc.thePlayer;
        if (localPlayer == null || localPlayer.worldObj == null) return;

        Integer disguiseId = transformationMap.get(localPlayer.getUniqueID());
        if (disguiseId == null) return;

        Entity disguise = localPlayer.worldObj.getEntityByID(disguiseId);
        if (!(disguise instanceof EntityDisguiseGecko)) return;

        // 在每一帧渲染开始时
        if (event.phase == TickEvent.Phase.START) {
            // 我们临时扩大伪装模型的边界框，让它能通过渲染管理器的可见性检查
            disguise.boundingBox.setBounds(
                    disguise.posX - 0.5D,
                    disguise.posY,
                    disguise.posZ - 0.5D,
                    disguise.posX + 0.5D,
                    disguise.posY + 2.0D,
                    disguise.posZ + 0.5D
            );
        }
        // 在每一帧渲染结束时
        else if (event.phase == TickEvent.Phase.END) {
            // 立刻将边界框恢复为0，确保它在下一个物理Tick中是完全隐形的
            disguise.boundingBox.setBounds(
                    disguise.posX, disguise.posY, disguise.posZ,
                    disguise.posX, disguise.posY, disguise.posZ
            );
        }

        // --- 客户端的“0延迟”视觉同步逻辑也在这里 ---
        if (disguise instanceof EntityLivingBase) {
            syncEntityStateFromClient(localPlayer, (EntityLivingBase) disguise, event.renderTickTime);
        }
    }


    /**
     * 在每个客户端Tick的开始，修正鼠标指向的目标，解决交互穿透问题
     */
    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer == null || mc.theWorld == null) {
                return;
            }

            // 检查玩家是否正在变身
            Integer disguiseId = transformationMap.get(mc.thePlayer.getUniqueID());
            if (disguiseId == null) {
                return;
            }

            // 检查当前鼠标是否正指向我们的伪装模型
            if (mc.objectMouseOver != null &&
                mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY &&
                mc.objectMouseOver.entityHit.getEntityId() == disguiseId) {

                // 如果是，就进行一次"修正"计算
                // 这个方法会暂时忽略伪装模型，重新进行一次射线追踪，
                // 然后用新的结果覆盖 mc.objectMouseOver
                fixMouseOver(mc);
            }
        }
    }

    /**
     * 核心修正方法：暂时忽略伪装模型，重新进行射线追踪，并更新mc.objectMouseOver
     */
    @SideOnly(Side.CLIENT)
    private void fixMouseOver(Minecraft mc) {
        EntityPlayer localPlayer = mc.thePlayer;
        Integer disguiseId = transformationMap.get(localPlayer.getUniqueID());
        if (disguiseId == null) return;

        Entity disguise = localPlayer.worldObj.getEntityByID(disguiseId);
        if (disguise == null) return;

        // 1. 保存伪装模型原始的碰撞箱
        AxisAlignedBB originalBoundingBox = disguise.boundingBox;

        // 2. 暂时将伪装模型的碰撞箱设置为一个无效的、极小的状态，使其无法被射线检测到
        //    我们不能设为null，因为那会导致崩溃。
        //    一个位于原点的零尺寸AABB是安全的。
        disguise.boundingBox.setBounds(0, 0, 0, 0, 0, 0);

        try {
            // 3. 让Minecraft重新计算一次鼠标指向的目标
            //    getMouseOver会更新mc.objectMouseOver
            //    由于我们已经"隐藏"了伪装模型的碰撞箱，这次计算就会穿透它。
            mc.entityRenderer.getMouseOver(1.0F);

        } finally {
            // 4. 无论如何，都要在最后把原始的碰撞箱恢复回去！
            disguise.boundingBox.setBB(originalBoundingBox);
        }
    }

//    @SideOnly(Side.CLIENT)
//    @SubscribeEvent
//    public void onPlayerHurt(LivingHurtEvent event) {
//        // 我们只关心客户端的本地玩家
//        Minecraft mc = Minecraft.getMinecraft();
//        if (mc.thePlayer == null || event.entityLiving.getEntityId() != mc.thePlayer.getEntityId()) {
//            return;
//        }
//
//        // 检查玩家是否正在变身
//        Integer disguiseId = transformationMap.get(mc.thePlayer.getUniqueID());
//        if (disguiseId != null) {
//            Entity disguise = mc.theWorld.getEntityByID(disguiseId);
//            if (disguise instanceof EntityLivingBase) {
//                // 手动触发伪装模型的受伤动画
//                // performHurtAnimation 是一个纯客户端的视觉方法
//                ((EntityLivingBase) disguise).performHurtAnimation();
//            }
//        }
//    }

    // 这个事件处理器在服务器和客户端都会运行
//    @SubscribeEvent
//    public void onDisguiseHurt(LivingHurtEvent event) {
//        // 如果事件发生在客户端，或者受伤的实体不是一个生物，直接返回
//        if (event.entity.worldObj.isRemote || !(event.entityLiving instanceof EntityLivingBase)) {
//            return;
//        }
//
//        // 检查受伤的实体ID是否在我们的变身模型的ID列表中
//        if (transformationMap.containsValue(event.entityLiving.getEntityId())) {
//            // 如果是，取消这个伤害事件，让它免疫
//            event.setCanceled(true);
//        }
//    }

    /**
     * 客户端的"0延迟"视觉同步方法
     * @param sourcePlayer 本地玩家
     * @param targetDisguise 伪装实体
     * @param partialTicks 渲染Tick的插值
     */
    @SideOnly(Side.CLIENT)
    public static void syncEntityStateFromClient(EntityPlayer sourcePlayer, EntityLivingBase targetDisguise, float partialTicks) {
        if (!sourcePlayer.isRiding()) {
            double interpX = sourcePlayer.lastTickPosX + (sourcePlayer.posX - sourcePlayer.lastTickPosX) * partialTicks;
            double playerBaseInterpY = sourcePlayer.lastTickPosY + (sourcePlayer.posY - sourcePlayer.lastTickPosY) * partialTicks;
            double interpY = playerBaseInterpY - sourcePlayer.yOffset - targetDisguise.yOffset;
            double interpZ = sourcePlayer.lastTickPosZ + (sourcePlayer.posZ - sourcePlayer.lastTickPosZ) * partialTicks;
            float interpYaw = sourcePlayer.prevRotationYaw + (sourcePlayer.rotationYaw - sourcePlayer.prevRotationYaw) * partialTicks;
            float interpPitch = sourcePlayer.prevRotationPitch + (sourcePlayer.rotationPitch - sourcePlayer.prevRotationPitch) * partialTicks;
            float interpRenderYawOffset = sourcePlayer.prevRenderYawOffset + (sourcePlayer.renderYawOffset - sourcePlayer.prevRenderYawOffset) * partialTicks;
            // 直接设置伪装实体的位置和朝向，欺骗渲染引擎
            targetDisguise.posX = interpX;
            targetDisguise.posY = interpY; // 使用修正后的 Y 坐标
            targetDisguise.posZ = interpZ;
            // 同时也更新 lastTickPos，防止实体在下一帧"抖动"
            targetDisguise.lastTickPosX = interpX;
            targetDisguise.lastTickPosY = interpY; // 同样使用修正后的 Y 坐标
            targetDisguise.lastTickPosZ = interpZ;
            targetDisguise.rotationYaw = interpYaw;
            targetDisguise.rotationPitch = interpPitch;
            targetDisguise.prevRotationPitch = interpPitch;
            targetDisguise.renderYawOffset = interpRenderYawOffset;
            // ... (所有关于 interpX, Y, Z, Yaw, Pitch, renderYawOffset 的计算和设置都放在这里) ...
        }

        // 头部转动和动画状态总是需要
        float interpYawHead = sourcePlayer.prevRotationYawHead + (sourcePlayer.rotationYawHead - sourcePlayer.prevRotationYawHead) * partialTicks;
        targetDisguise.rotationYawHead = interpYawHead;
        targetDisguise.prevRotationYawHead = interpYawHead;

        // 3. 实时同步动画变量 (这是无缝体验的关键！)
        // 注意：动画变量通常不需要插值，直接复制当前状态即可
        targetDisguise.limbSwing = sourcePlayer.limbSwing;
        targetDisguise.limbSwingAmount = sourcePlayer.limbSwingAmount;
        targetDisguise.prevLimbSwingAmount = sourcePlayer.prevLimbSwingAmount; // 这个也很重要
        targetDisguise.swingProgress = sourcePlayer.swingProgress;
        targetDisguise.isSwingInProgress = sourcePlayer.isSwingInProgress;
        targetDisguise.setSneaking(sourcePlayer.isSneaking());
        targetDisguise.setSprinting(sourcePlayer.isSprinting());
        targetDisguise.onGround = sourcePlayer.onGround;
        targetDisguise.motionY = sourcePlayer.motionY;
        if (targetDisguise instanceof EntityDisguiseGecko) {
            EntityDisguiseGecko geckoDisguise = (EntityDisguiseGecko) targetDisguise;
            // 1. 同步飞行状态
            geckoDisguise.setFlying(sourcePlayer.capabilities.isFlying);
            // 2. 同步睡觉状态
            geckoDisguise.setSleeping(sourcePlayer.isPlayerSleeping());
        }
        if (sourcePlayer.isRiding()) {
            // 简单地同步ID可能不足以让客户端正确渲染，但这是服务器逻辑的基础
            if (targetDisguise.ridingEntity == null || targetDisguise.ridingEntity.getEntityId() != sourcePlayer.ridingEntity.getEntityId()) {
                // 注意：直接设置ridingEntity可能很复杂，更好的方式是让实体自己寻找并骑乘
                // 但对于纯视觉模型，我们主要同步状态
                targetDisguise.ridingEntity = sourcePlayer.ridingEntity;
            }
        } else {
            targetDisguise.ridingEntity = null;
        }
        targetDisguise.isOnLadder();
    }
}