package com.fox.ysmu.command;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentTranslation;

import com.fox.ysmu.command.sub.*;

public class RootCommand extends CommandBase {

    private static final String ROOT_NAME = "ysm";

    @Override
    public String getCommandName() {
        return ROOT_NAME;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/" + ROOT_NAME + " <subcommand>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.addChatMessage(new ChatComponentTranslation("commands.yes_steve_model.no_subcommand"));
            return;
        }

        String subCommand = args[0];
        String[] subArgs = new String[args.length - 1];
        System.arraycopy(args, 1, subArgs, 0, subArgs.length);

        // 处理各个子命令
        if ("auth".equals(subCommand)) {
            new AuthCommand().processCommand(sender, subArgs);
        } else if ("export".equals(subCommand)) {
            new ExportCommand().processCommand(sender, subArgs);
        } else if ("manage".equals(subCommand)) {
            new ManageCommand().processCommand(sender, subArgs);
        } else if ("model".equals(subCommand)) {
            new ModelCommand().processCommand(sender, subArgs);
        } else if ("play".equals(subCommand)) {
            new PlayAnimationCommand().processCommand(sender, subArgs);
        } else {
            sender.addChatMessage(
                new ChatComponentTranslation("commands.yes_steve_model.invalid_subcommand", subCommand));
        }
    }

    @Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>();
            subCommands.add("auth");
            subCommands.add("export");
            subCommands.add("manage");
            subCommands.add("model");
            subCommands.add("play");
            return subCommands;
        }
        return null;
    }
}
