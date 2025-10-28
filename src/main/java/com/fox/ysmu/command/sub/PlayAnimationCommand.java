package com.fox.ysmu.command.sub;

import com.fox.ysmu.eep.ExtendedModelInfo;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerSelector;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;

import java.util.List;

public class PlayAnimationCommand extends CommandBase {
    private static final String PLAY_NAME = "play";
    private static final String STOP = "stop";

    @Override
    public String getCommandName() {
        return PLAY_NAME;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/" + PLAY_NAME + " <targets> <animation>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.addChatMessage(new ChatComponentTranslation("commands.yes_steve_model.play.usage"));
            return;
        }

        String targetSelector = args[0];
        String animation = args[1];

        try {
            List<EntityPlayerMP> targets = PlayerSelector.matchPlayers(sender, targetSelector);
            if (targets.isEmpty()) {
                sender.addChatMessage(new ChatComponentTranslation("commands.yes_steve_model.play.no_targets"));
                return;
            }

            playAnimation(sender, targets, animation);
        } catch (Exception e) {
            sender.addChatMessage(new ChatComponentTranslation("commands.yes_steve_model.play.error", e.getMessage()));
        }
    }

    private void playAnimation(ICommandSender sender, List<EntityPlayerMP> targets, String animation) {
        for (EntityPlayerMP player : targets) {
            ExtendedModelInfo eep = ExtendedModelInfo.get(player);
            if (eep != null) {
                if (STOP.equals(animation)) {
                    eep.stopAnimation();
                } else {
                    eep.playAnimation(animation);
                }
            }
        }
        sender.addChatMessage(new ChatComponentTranslation("commands.yes_steve_model.play.success"));
    }

    @Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, MinecraftServer.getServer().getAllUsernames());
        }
        return null;
    }
}
