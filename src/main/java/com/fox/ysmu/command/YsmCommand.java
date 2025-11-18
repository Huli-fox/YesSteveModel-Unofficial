package com.fox.ysmu.command;

import static com.fox.ysmu.compat.Utils.isValidResourceLocation;
import static com.fox.ysmu.model.ServerModelManager.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

import net.minecraft.command.*;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ResourceLocation;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;

import com.fox.ysmu.eep.ExtendedAuthModels;
import com.fox.ysmu.eep.ExtendedModelInfo;
import com.fox.ysmu.model.ServerModelManager;
import com.fox.ysmu.ysmu;

public class YsmCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "ysm";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/ysm reload";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length != 1 || !"reload".equalsIgnoreCase(args[0])) {
            throw new WrongUsageException(getCommandUsage(sender));
        }
        processReload(sender);
    }

    private void processReload(ICommandSender sender) {
        StopWatch watch = new StopWatch();
        watch.start();
        checkModelFiles(sender, CUSTOM);
        checkModelFiles(sender, AUTH);
        ServerModelManager.reloadPacks();

        // TODO 需要根据加载环境判断是客户端还是服务端
        if (MinecraftServer.getServer().isDedicatedServer()) {
            ServerModelManager.sendRequestSyncModelMessage(sender.getEntityWorld().playerEntities);
        } else {
            ServerModelManager.sendRequestSyncModelMessage();
        }

        List<EntityPlayerMP> players = MinecraftServer.getServer().getConfigurationManager().playerEntityList;
        for (EntityPlayerMP player : players) {
            ExtendedAuthModels ownModelsEEP = ExtendedAuthModels.get(player);
            if (ownModelsEEP != null) {
                ExtendedModelInfo modelIdEEP = ExtendedModelInfo.get(player);
                if (modelIdEEP != null) {
                    if (ServerModelManager.AUTH_MODELS.contains(modelIdEEP.getModelId().getResourcePath()) && !ownModelsEEP.containModel(modelIdEEP.getModelId())) {
                        ResourceLocation defaultModelId = new ResourceLocation(ysmu.MODID, "default");
                        ResourceLocation defaultTextureId = new ResourceLocation(ysmu.MODID, "default/default.png");
                        modelIdEEP.setModelAndTexture(defaultModelId, defaultTextureId);
                    }
                }
            }
        }
        watch.stop();
        sender.addChatMessage(new ChatComponentTranslation("message.yes_steve_model.model.reload.info", watch.getTime()));
    }

    private void checkModelFiles(ICommandSender sender, Path rootPath) {
        Collection<File> dirs = FileUtils.listFiles(rootPath.toFile(), DirectoryFileFilter.INSTANCE, null);
        for (File dir : dirs) {
            String dirName = dir.getName();
            if (!isValidResourceLocation(dirName)) {
                sender.addChatMessage(new ChatComponentTranslation("message.yes_steve_model.model.reload.error.dir_name", dirName));
            }
            boolean noMainModelFile = true;
            boolean noArmModelFile = true;
            boolean noTextureFile = true;
            Collection<File> files = FileUtils.listFiles(rootPath.resolve(dirName).toFile(), FileFileFilter.FILE, null);
            for (File file : files) {
                String fileName = file.getName();
                if (MAIN_MODEL_FILE_NAME.equals(fileName) && isNotBlankFile(file)) {
                    noMainModelFile = false;
                }
                if (ARM_MODEL_FILE_NAME.equals(fileName) && isNotBlankFile(file)) {
                    noArmModelFile = false;
                }
                if (fileName.endsWith(".png")) {
                    noTextureFile = false;
                    String name = file.getName();
                    name = name.substring(0, name.length() - 4);
                    if (!isValidResourceLocation(name)) {
                        String showName = String.format("%s/%s.png", dirName, name);
                        sender.addChatMessage(new ChatComponentTranslation("message.yes_steve_model.model.reload.error.texture_name", showName));
                    }
                }
            }
            if (noMainModelFile) {
                sender.addChatMessage(new ChatComponentTranslation("message.yes_steve_model.model.reload.error.no_main_file", dirName));
            }
            if (noArmModelFile) {
                sender.addChatMessage(new ChatComponentTranslation("message.yes_steve_model.model.reload.error.no_arm_file", dirName));
            }
            if (noTextureFile) {
                sender.addChatMessage(new ChatComponentTranslation("message.yes_steve_model.model.reload.error.no_texture_file", dirName));
            }
        }
    }

    private static boolean isNotBlankFile(File file) {
        try {
            String fileText = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
            return StringUtils.isNoneBlank(fileText);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
