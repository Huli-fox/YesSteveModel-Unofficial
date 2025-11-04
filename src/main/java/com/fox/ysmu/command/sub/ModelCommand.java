package com.fox.ysmu.command.sub;

import static com.fox.ysmu.model.ServerModelManager.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerSelector;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ResourceLocation;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;

import com.fox.ysmu.compat.Utils;
import com.fox.ysmu.eep.ExtendedAuthModels;
import com.fox.ysmu.eep.ExtendedModelInfo;
import com.fox.ysmu.model.ServerModelManager;
import com.fox.ysmu.model.format.ServerModelInfo;
import com.fox.ysmu.util.ModelIdUtil;
import com.fox.ysmu.ysmu;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;

public class ModelCommand extends CommandBase {

    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping()
        .excludeFieldsWithoutExposeAnnotation()
        .create();
    private static final String MODEL_NAME = "model";

    @Override
    public String getCommandName() {
        return MODEL_NAME;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/" + MODEL_NAME + " <reload|set|export> ...";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.addChatMessage(new ChatComponentTranslation("commands.yes_steve_model.model.usage"));
            return;
        }

        String subCommand = args[0];

        try {
            switch (subCommand) {
                case "reload":
                    reloadAllPack(sender);
                    break;
                case "set":
                    if (args.length < 4) {
                        sender.addChatMessage(new ChatComponentTranslation("commands.yes_steve_model.model.set.usage"));
                        return;
                    }
                    String targetSelector = args[1];
                    String modelName = args[2];
                    String textureName = args[3];
                    boolean ignoreAuth = args.length >= 5 && "true".equals(args[4]);
                    List<EntityPlayerMP> targets = Arrays.asList(PlayerSelector.matchPlayers(sender, targetSelector));
                    if (targets.isEmpty()) {
                        sender.addChatMessage(
                            new ChatComponentTranslation("commands.yes_steve_model.model.set.no_targets"));
                        return;
                    }
                    setModel(sender, targets, modelName, textureName, ignoreAuth);
                    break;
                case "export":
                    exportAllPackInfo(sender);
                    break;
                default:
                    sender.addChatMessage(
                        new ChatComponentTranslation("commands.yes_steve_model.model.invalid_subcommand"));
                    break;
            }
        } catch (Exception e) {
            sender.addChatMessage(new ChatComponentTranslation("commands.yes_steve_model.model.error", e.getMessage()));
        }
    }

    private void setModel(ICommandSender sender, List<EntityPlayerMP> targets, String modelName, String textureName,
        boolean ignoreAuth) {
        if (!ServerModelManager.CACHE_NAME_INFO.containsKey(modelName)) {
            sender.addChatMessage(new ChatComponentTranslation("commands.yes_steve_model.export.not_exist", modelName));
            return;
        }

        ServerModelInfo info = ServerModelManager.CACHE_NAME_INFO.get(modelName);
        if (!info.getTexture()
            .isPresent()) {
            return;
        }

        ResourceLocation modelId = new ResourceLocation(ysmu.MODID, modelName);
        ResourceLocation textureId = ModelIdUtil.getSubModelId(modelId, textureName);

        if (ignoreAuth) {
            for (EntityPlayerMP player : targets) {
                ExtendedModelInfo eep = ExtendedModelInfo.get(player);
                if (eep != null) {
                    eep.setModelAndTexture(modelId, textureId);
                    sender.addChatMessage(
                        new ChatComponentTranslation(
                            "message.yes_steve_model.model.set.success",
                            modelName,
                            player.getCommandSenderName()));
                }
            }
            return;
        }

        for (EntityPlayerMP player : targets) {
            ExtendedModelInfo eep = ExtendedModelInfo.get(player);
            ExtendedAuthModels authEEP = ExtendedAuthModels.get(player);
            if (eep != null && authEEP != null) {
                if (!ServerModelManager.AUTH_MODELS.contains(modelName) || authEEP.containModel(modelId)) {
                    eep.setModelAndTexture(modelId, textureId);
                    sender.addChatMessage(
                        new ChatComponentTranslation(
                            "message.yes_steve_model.model.set.success",
                            modelName,
                            player.getCommandSenderName()));
                } else {
                    sender.addChatMessage(
                        new ChatComponentTranslation(
                            "message.yes_steve_model.model.set.need_auth",
                            modelName,
                            player.getCommandSenderName()));
                }
            }
        }
    }

    private void exportAllPackInfo(ICommandSender sender) {
        String infoText = GSON.toJson(ServerModelManager.CACHE_NAME_INFO);
        sender.addChatMessage(new ChatComponentTranslation("commands.yes_steve_model.model.export.info", infoText));
    }

    private void reloadAllPack(ICommandSender sender) {
        StopWatch watch = new StopWatch();
        watch.start();
        checkModelFiles(sender, CUSTOM);
        checkModelFiles(sender, AUTH);
        ServerModelManager.reloadPacks();
        if (FMLCommonHandler.instance()
            .getEffectiveSide() == Side.SERVER) { // TODO 危险的双端判断
            ServerModelManager.sendRequestSyncModelMessage(sender.getEntityWorld().playerEntities);
        } else {
            ServerModelManager.sendRequestSyncModelMessage();
        }
        // 获取所有在线玩家并处理
        for (Object obj : MinecraftServer.getServer()
            .getConfigurationManager().playerEntityList) {
            if (obj instanceof EntityPlayerMP) {
                EntityPlayerMP player = (EntityPlayerMP) obj;
                ExtendedAuthModels ownModelsEEP = ExtendedAuthModels.get(player);
                ExtendedModelInfo modelIdEEP = ExtendedModelInfo.get(player);
                if (ownModelsEEP != null && modelIdEEP != null) {
                    if (ServerModelManager.AUTH_MODELS.contains(
                        modelIdEEP.getModelId()
                            .getResourcePath())
                        && !ownModelsEEP.containModel(modelIdEEP.getModelId())) {
                        ResourceLocation defaultModelId = new ResourceLocation(ysmu.MODID, "default");
                        ResourceLocation defaultTextureId = new ResourceLocation(ysmu.MODID, "default/default.png");
                        modelIdEEP.setModelAndTexture(defaultModelId, defaultTextureId);
                    }
                }
            }
        }
        watch.stop();
        sender
            .addChatMessage(new ChatComponentTranslation("message.yes_steve_model.model.reload.info", watch.getTime()));
    }

    private void checkModelFiles(ICommandSender sender, Path rootPath) {
        Collection<File> dirs = FileUtils.listFiles(rootPath.toFile(), DirectoryFileFilter.INSTANCE, null);
        for (File dir : dirs) {
            String dirName = dir.getName();
            if (!Utils.isValidResourceLocation(dirName)) {
                sender.addChatMessage(
                    new ChatComponentTranslation("message.yes_steve_model.model.reload.error.dir_name", dirName));
            }
            boolean noMainModelFile = true;
            boolean noArmModelFile = true;
            boolean noTextureFile = true;
            Collection<File> files = FileUtils.listFiles(
                rootPath.resolve(dirName)
                    .toFile(),
                FileFileFilter.FILE,
                null);
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
                    if (!Utils.isValidResourceLocation(name)) {
                        String showName = String.format("%s/%s.png", dirName, name);
                        sender.addChatMessage(
                            new ChatComponentTranslation(
                                "message.yes_steve_model.model.reload.error.texture_name",
                                showName));
                    }
                }
            }
            if (noMainModelFile) {
                sender.addChatMessage(
                    new ChatComponentTranslation("message.yes_steve_model.model.reload.error.no_main_file", dirName));
            }
            if (noArmModelFile) {
                sender.addChatMessage(
                    new ChatComponentTranslation("message.yes_steve_model.model.reload.error.no_arm_file", dirName));
            }
            if (noTextureFile) {
                sender.addChatMessage(
                    new ChatComponentTranslation(
                        "message.yes_steve_model.model.reload.error.no_texture_file",
                        dirName));
            }
        }
    }

    private boolean isNotBlankFile(File file) {
        try {
            String fileText = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
            return StringUtils.isNoneBlank(fileText);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            options.add("reload");
            options.add("set");
            options.add("export");
            return options;
        }

        if (args.length >= 2) {
            String subCommand = args[0];
            if ("set".equals(subCommand) && args.length == 2) {
                // 返回在线玩家列表
                return getListOfStringsMatchingLastWord(
                    args,
                    MinecraftServer.getServer()
                        .getAllUsernames());
            }
        }

        return null;
    }
}
