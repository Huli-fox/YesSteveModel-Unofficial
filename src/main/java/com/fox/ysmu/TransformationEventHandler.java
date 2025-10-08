package com.fox.ysmu;

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
        // 1. 位置和朝向 (这是最重要的)
        // 修正Y坐标以解决悬浮问题
        double correctedY = sourcePlayer.posY - sourcePlayer.yOffset - targetDisguise.yOffset;
        
        targetDisguise.setPositionAndRotation(
                sourcePlayer.posX,
                correctedY, // 使用修正后的 Y 坐标
                sourcePlayer.posZ,
                sourcePlayer.rotationYaw,
                sourcePlayer.rotationPitch
        );
        // 同步头部转动，这对于僵尸、骷髅等很重要
        targetDisguise.rotationYawHead = sourcePlayer.rotationYawHead;

        // 2. 运动状态
        targetDisguise.motionX = sourcePlayer.motionX;
        targetDisguise.motionY = sourcePlayer.motionY;
        targetDisguise.motionZ = sourcePlayer.motionZ;

        // 3. 动画状态 (这些是让实体"活"起来的关键)
        targetDisguise.limbSwing = sourcePlayer.limbSwing;
        targetDisguise.limbSwingAmount = sourcePlayer.limbSwingAmount;
        targetDisguise.swingProgress = sourcePlayer.swingProgress;
        targetDisguise.isSwingInProgress = sourcePlayer.isSwingInProgress;

        // 4. 其他状态
        targetDisguise.setSneaking(sourcePlayer.isSneaking());
        targetDisguise.setSprinting(sourcePlayer.isSprinting());
        if (sourcePlayer.isBurning()) {
            targetDisguise.setFire(10);
        } else {
            targetDisguise.extinguish();
        }
        // 更多状态... (例如是否隐身，是否在骑乘等)
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
        // 我们在每一帧的开始进行同步
        if (event.phase == TickEvent.Phase.START) {
            Minecraft mc = Minecraft.getMinecraft();
            EntityPlayer localPlayer = mc.thePlayer;
            World clientWorld = mc.theWorld;

            if (localPlayer == null || clientWorld == null) return;

            // 检查本地玩家是否正在变身
            Integer disguiseId = transformationMap.get(localPlayer.getUniqueID());
            if (disguiseId == null) return;

            Entity disguise = getEntityById(disguiseId, clientWorld);

            if (disguise instanceof EntityLivingBase) {
                // 使用 partialTicks 进行平滑的、0延迟的同步
                syncEntityStateFromClient(localPlayer, (EntityLivingBase) disguise, event.renderTickTime);
            }
        }
    }

    /**
     * 在生物即将被渲染时进行拦截，用于处理第一人称穿模问题。
     */
    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onLivingRender(RenderLivingEvent.Pre event) {
        // 获取即将被渲染的实体
        EntityLivingBase entityBeingRendered = event.entity;

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer localPlayer = mc.thePlayer;

        // 如果本地玩家不存在，或者游戏世界不存在，则不做任何事
        if (localPlayer == null || mc.theWorld == null) {
            return;
        }

        // 检查本地玩家是否正在变身
        Integer disguiseId = transformationMap.get(localPlayer.getUniqueID());
        if (disguiseId == null) {
            return; // 玩家没有变身，直接返回
        }

        // 判断：即将被渲染的实体，是不是我们玩家的伪装实体？
        if (entityBeingRendered.getEntityId() == disguiseId) {

            // 关键判断：当前是否为第一人称视角？
            // mc.gameSettings.thirdPersonView == 0 代表第一人称
            if (mc.gameSettings.thirdPersonView == 0) {
                // 是第一人称，且正在渲染我们自己的伪装模型，
                // 那就取消这次渲染，防止看到模型内部！
                event.setCanceled(true);
            }
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

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onPlayerHurt(LivingHurtEvent event) {
        // 我们只关心客户端的本地玩家
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || event.entityLiving.getEntityId() != mc.thePlayer.getEntityId()) {
            return;
        }

        // 检查玩家是否正在变身
        Integer disguiseId = transformationMap.get(mc.thePlayer.getUniqueID());
        if (disguiseId != null) {
            Entity disguise = mc.theWorld.getEntityByID(disguiseId);
            if (disguise instanceof EntityLivingBase) {
                // 手动触发伪装模型的受伤动画
                // performHurtAnimation 是一个纯客户端的视觉方法
                ((EntityLivingBase) disguise).performHurtAnimation();
            }
        }
    }

    // 这个事件处理器在服务器和客户端都会运行
    @SubscribeEvent
    public void onDisguiseHurt(LivingHurtEvent event) {
        // 如果事件发生在客户端，或者受伤的实体不是一个生物，直接返回
        if (event.entity.worldObj.isRemote || !(event.entityLiving instanceof EntityLivingBase)) {
            return;
        }

        // 检查受伤的实体ID是否在我们的变身模型的ID列表中
        if (transformationMap.containsValue(event.entityLiving.getEntityId())) {
            // 如果是，取消这个伤害事件，让它免疫
            event.setCanceled(true);
        }
    }

    /**
     * 客户端的"0延迟"视觉同步方法
     * @param sourcePlayer 本地玩家
     * @param targetDisguise 伪装实体
     * @param partialTicks 渲染Tick的插值
     */
    @SideOnly(Side.CLIENT)
    public static void syncEntityStateFromClient(EntityPlayer sourcePlayer, EntityLivingBase targetDisguise, float partialTicks) {
        // 1. 计算玩家在当前帧的精确（插值）位置和朝向
        double interpX = sourcePlayer.lastTickPosX + (sourcePlayer.posX - sourcePlayer.lastTickPosX) * partialTicks;
        
        // 计算包含 yOffset 的插值 Y 坐标以解决悬浮问题
        double playerBaseInterpY = sourcePlayer.lastTickPosY + (sourcePlayer.posY - sourcePlayer.lastTickPosY) * partialTicks;
        double interpY = playerBaseInterpY - sourcePlayer.yOffset - targetDisguise.yOffset;

        double interpZ = sourcePlayer.lastTickPosZ + (sourcePlayer.posZ - sourcePlayer.lastTickPosZ) * partialTicks;

        float interpYaw = sourcePlayer.prevRotationYaw + (sourcePlayer.rotationYaw - sourcePlayer.prevRotationYaw) * partialTicks;
        float interpPitch = sourcePlayer.prevRotationPitch + (sourcePlayer.rotationPitch - sourcePlayer.prevRotationPitch) * partialTicks;
        float interpYawHead = sourcePlayer.prevRotationYawHead + (sourcePlayer.rotationYawHead - sourcePlayer.prevRotationYawHead) * partialTicks;

        // 2. 直接设置伪装实体的位置和朝向，欺骗渲染引擎
        targetDisguise.posX = interpX;
        targetDisguise.posY = interpY; // 使用修正后的 Y 坐标
        targetDisguise.posZ = interpZ;
        // 同时也更新 lastTickPos，防止实体在下一帧"抖动"
        targetDisguise.lastTickPosX = interpX;
        targetDisguise.lastTickPosY = interpY; // 同样使用修正后的 Y 坐标
        targetDisguise.lastTickPosZ = interpZ;

        targetDisguise.rotationYaw = interpYaw;
        targetDisguise.rotationPitch = interpPitch;
        targetDisguise.rotationYawHead = interpYawHead;

        // 3. 实时同步动画变量 (这是无缝体验的关键！)
        // 注意：动画变量通常不需要插值，直接复制当前状态即可
        targetDisguise.limbSwing = sourcePlayer.limbSwing;
        targetDisguise.limbSwingAmount = sourcePlayer.limbSwingAmount;
        targetDisguise.prevLimbSwingAmount = sourcePlayer.prevLimbSwingAmount; // 这个也很重要

        targetDisguise.swingProgress = sourcePlayer.swingProgress;
        targetDisguise.isSwingInProgress = sourcePlayer.isSwingInProgress;

        // 4. 实时同步状态
        targetDisguise.setSneaking(sourcePlayer.isSneaking());
        targetDisguise.setSprinting(sourcePlayer.isSprinting());

        // 如果是僵尸，可能需要手动设置手臂的姿态
        if (targetDisguise instanceof net.minecraft.entity.monster.EntityZombie) {
            // 你可以在这里根据玩家是否在攻击来设置僵尸的手臂举起状态
            // ((net.minecraft.entity.monster.EntityZombie) targetDisguise).setArmsRaised(sourcePlayer.isSwingInProgress);
        }
    }
}