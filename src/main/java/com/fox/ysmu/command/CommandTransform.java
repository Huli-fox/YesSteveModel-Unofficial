package com.fox.ysmu.command;

import com.fox.ysmu.entity.EntityDisguiseGecko; // 确保导入你自己的实体类
import com.fox.ysmu.TransformationEventHandler;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;


public class CommandTransform extends CommandBase {

    @Override
    public String getCommandName() {
        return "transform";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/transform <model_name|clear>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0; // 0代表所有人可用, 2代表仅OP可用
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (!(sender instanceof EntityPlayerMP)) {
            sender.addChatMessage(new ChatComponentText("This command can only be run by a player."));
            return;
        }

        EntityPlayerMP player = (EntityPlayerMP) sender;
        World world = player.worldObj;

        // 处理清除变身的逻辑
        if (args.length > 0 && args[0].equalsIgnoreCase("clear")) {
            Integer oldEntityId = TransformationEventHandler.transformationMap.remove(player.getUniqueID());
            if (oldEntityId != null) {
                Entity oldDisguise = world.getEntityByID(oldEntityId);
                if (oldDisguise != null) {
                    oldDisguise.setDead();
                }
                player.addChatMessage(new ChatComponentText("Transformation cleared."));
            }
            return;
        }

        if (args.length < 1) {
            sender.addChatMessage(new ChatComponentText(getCommandUsage(sender)));
            return;
        }

        // --- 以下是全新的、正确的变身逻辑 ---

        // 1. 先清除任何旧的变身
        Integer oldEntityId = TransformationEventHandler.transformationMap.remove(player.getUniqueID());
        if (oldEntityId != null) {
            Entity oldDisguise = world.getEntityByID(oldEntityId);
            if (oldDisguise != null) {
                oldDisguise.setDead();
            }
        }

        String modelName = args[0];

        // 2. 创建我们唯一的、通用的变身实体
        EntityDisguiseGecko disguise = new EntityDisguiseGecko(world);

        // 3. 告诉这个实体它应该使用哪个模型
        disguise.setModel(modelName);

        // 4. 设置位置并将其生成在世界中
        disguise.setPosition(player.posX, player.posY, player.posZ);
        world.spawnEntityInWorld(disguise);

        // 5. 更新我们的追踪Map，让事件处理器知道该同步哪个实体
        TransformationEventHandler.transformationMap.put(player.getUniqueID(), disguise.getEntityId());

        player.addChatMessage(new ChatComponentText("You have transformed into: " + modelName));
    }
}