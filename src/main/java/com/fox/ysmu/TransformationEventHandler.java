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
import java.util.Iterator;
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

            Iterator<Map.Entry<UUID, Integer>> iterator = transformationMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<UUID, Integer> entry = iterator.next();
                UUID playerUUID = entry.getKey();
                Integer entityId = entry.getValue();

                // 找到对应的玩家和实体
                EntityPlayer player = getPlayerByUUID(playerUUID);
                // 如果玩家不存在
                if (player == null) {
                    // 这个映射已失效，从Map中移除
                    iterator.remove();
                    // 顺便尝试移除可能残留的实体
                    Entity oldDisguise = getEntityByIdAcrossAllWorlds(entityId);
                    if (oldDisguise != null) {
                        oldDisguise.setDead();
                    }
                    continue; // 处理下一个
                }

                Entity disguise = getEntityById(entityId, player.worldObj);
                // 如果实体不存在
                if (disguise == null) {
                    // 这个映射也已失效，移除它
                    iterator.remove();
                    continue; // 处理下一个
                }

                // 关键修改：删除伪装实体的骑乘状态同步，仅保留玩家的骑乘状态用于动画
                if (player.isRiding()) {
                    // 直接同步伪装实体位置到玩家的骑乘位置（不建立骑乘关系）
                    Entity vehicle = player.ridingEntity;
                    // 计算玩家在交通工具上的相对位置
                    double relX = player.posX - vehicle.posX;
                    double relY = player.posY - vehicle.posY;
                    double relZ = player.posZ - vehicle.posZ;
                    // 伪装实体跟随玩家的相对位置
                    disguise.setPosition(
                            vehicle.posX + relX,
                            vehicle.posY + relY,
                            vehicle.posZ + relZ
                    );
                } else {
                    // 非骑乘状态正常同步位置
                    if (disguise instanceof EntityLivingBase) {
                        syncEntityStateFromServer(player, (EntityLivingBase) disguise);
                    } else {
                        disguise.setPositionAndRotation(player.posX, player.posY, player.posZ, player.rotationYaw, player.rotationPitch);
                    }
                }

            }
        }
    }
    //在玩家下线后清理实体
    private Entity getEntityByIdAcrossAllWorlds(int entityId) {
        MinecraftServer server = MinecraftServer.getServer();
        if (server != null) {
            for (net.minecraft.world.WorldServer world : server.worldServers) {
                Entity entity = world.getEntityByID(entityId);
                if (entity != null) {
                    return entity;
                }
            }
        }
        return null;
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
        } else {
            // 骑乘时仅同步位置，不设置ridingEntity
            Entity vehicle = sourcePlayer.ridingEntity;
            if (vehicle != null) {
                double relX = sourcePlayer.posX - vehicle.posX;
                double relY = sourcePlayer.posY - vehicle.posY;
                double relZ = sourcePlayer.posZ - vehicle.posZ;
                targetDisguise.setPosition(vehicle.posX + relX, vehicle.posY + relY, vehicle.posZ + relZ);
                targetDisguise.motionX = 0.0D;
                targetDisguise.motionY = 0.0D;
                targetDisguise.motionZ = 0.0D;
                targetDisguise.onGround = true;
            }
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
            // 3. 同步骑乘状态
            if (sourcePlayer.isRiding()) {
                Entity vehicle = sourcePlayer.ridingEntity;
                if (vehicle instanceof net.minecraft.entity.item.EntityMinecart || vehicle instanceof net.minecraft.entity.item.EntityBoat) {
                    geckoDisguise.setRidingState(1); // 坐
                } else if (vehicle instanceof net.minecraft.entity.passive.EntityHorse) {
                    geckoDisguise.setRidingState(2); // 骑马
                } else if (vehicle instanceof net.minecraft.entity.passive.EntityPig) {
                    geckoDisguise.setRidingState(3); // 骑猪
                } else {
                    geckoDisguise.setRidingState(1); // 默认为坐
                }
            } else {
                geckoDisguise.setRidingState(0); // 不骑乘
            }
        }
        targetDisguise.ridingEntity = null;
        targetDisguise.isOnLadder();

        // 装备同步逻辑
        if (targetDisguise instanceof EntityDisguiseGecko) {
            for (int i = 0; i < 5; ++i) {
                // 将玩家的装备复制到伪装实体中
                ((EntityDisguiseGecko) targetDisguise).setCurrentItemOrArmor(i, sourcePlayer.getEquipmentInSlot(i));
            }
        }
    }

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
        } else {
            Entity vehicle = sourcePlayer.ridingEntity;
            if (vehicle != null) {
                // 计算玩家的插值相对位置
                double playerInterpX = sourcePlayer.lastTickPosX + (sourcePlayer.posX - sourcePlayer.lastTickPosX) * partialTicks;
                double playerInterpY = sourcePlayer.lastTickPosY + (sourcePlayer.posY - sourcePlayer.lastTickPosY) * partialTicks;
                double playerInterpZ = sourcePlayer.lastTickPosZ + (sourcePlayer.posZ - sourcePlayer.lastTickPosZ) * partialTicks;

                // 计算交通工具的插值位置
                double vehicleInterpX = vehicle.lastTickPosX + (vehicle.posX - vehicle.lastTickPosX) * partialTicks;
                double vehicleInterpY = vehicle.lastTickPosY + (vehicle.posY - vehicle.lastTickPosY) * partialTicks;
                double vehicleInterpZ = vehicle.lastTickPosZ + (vehicle.posZ - vehicle.lastTickPosZ) * partialTicks;

                double relX = playerInterpX - vehicleInterpX;
                double relY = playerInterpY - vehicleInterpY;
                double relZ = playerInterpZ - vehicleInterpZ;

                // 1. 手动同步插值后的位置，并应用Y轴偏移
                double correctedRideY = vehicleInterpY + relY - sourcePlayer.yOffset - targetDisguise.yOffset;
                targetDisguise.posX = vehicleInterpX + relX;
                targetDisguise.posY = correctedRideY;
                targetDisguise.posZ = vehicleInterpZ + relZ;

                // 2. 【核心修复】冻结客户端的物理状态
                targetDisguise.motionX = 0.0D;
                targetDisguise.motionY = 0.0D;
                targetDisguise.motionZ = 0.0D;
                targetDisguise.onGround = true;
            }
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
            // 3. 同步骑乘状态
            if (sourcePlayer.isRiding()) {
                Entity vehicle = sourcePlayer.ridingEntity;
                if (vehicle instanceof net.minecraft.entity.item.EntityMinecart || vehicle instanceof net.minecraft.entity.item.EntityBoat) {
                    geckoDisguise.setRidingState(1); // 坐
                } else if (vehicle instanceof net.minecraft.entity.passive.EntityHorse) {
                    geckoDisguise.setRidingState(2); // 骑马
                } else if (vehicle instanceof net.minecraft.entity.passive.EntityPig) {
                    geckoDisguise.setRidingState(3); // 骑猪
                } else {
                    geckoDisguise.setRidingState(1); // 默认为坐
                }
            } else {
                geckoDisguise.setRidingState(0); // 不骑乘
            }
        }
        targetDisguise.isOnLadder();

        // 装备同步逻辑
        if (targetDisguise instanceof EntityDisguiseGecko) {
            for (int i = 0; i < 5; ++i) {
                ((EntityDisguiseGecko) targetDisguise).setCurrentItemOrArmor(i, sourcePlayer.getEquipmentInSlot(i));
            }
        }
    }
}