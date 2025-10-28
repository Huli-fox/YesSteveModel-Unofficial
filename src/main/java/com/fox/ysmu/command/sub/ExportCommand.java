package com.fox.ysmu.command.sub;

import com.fox.ysmu.ysmu;
import com.fox.ysmu.model.ServerModelManager;
import com.fox.ysmu.util.YesModelUtils;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentTranslation;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ExportCommand extends CommandBase {
    private static final String EXPORT_NAME = "export";

    @Override
    public String getCommandName() {
        return EXPORT_NAME;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/" + EXPORT_NAME + " <model_id>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.addChatMessage(new ChatComponentTranslation("commands.yes_steve_model.export.usage"));
            return;
        }

        String modelName = args[0];
        File customFolder = ServerModelManager.CUSTOM.resolve(modelName).toFile();
        if (customFolder.isDirectory()) {
            try {
                YesModelUtils.export(customFolder);
                sender.addChatMessage(new ChatComponentTranslation("commands.yes_steve_model.export.success", ysmu.MODID, modelName));
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        File authFolder = ServerModelManager.AUTH.resolve(modelName).toFile();
        if (authFolder.isDirectory()) {
            try {
                YesModelUtils.export(authFolder);
                sender.addChatMessage(new ChatComponentTranslation("commands.yes_steve_model.export.success", ysmu.MODID, modelName));
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        sender.addChatMessage(new ChatComponentTranslation("commands.yes_steve_model.export.not_exist", modelName));
    }

    @Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        // 在这个版本中不实现自动补全功能
        return null;
    }
}