package com.fox.ysmu.command.sub;

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
import net.minecraft.util.ChatComponentText;
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

    @Override
    public String getCommandName() {
        return "model";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/ysm model <reload|set|export> ...";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length < 1) {
            throw new WrongUsageException(getCommandUsage(sender));
        }

        String subCommand = args[0];

        if ("reload".equalsIgnoreCase(subCommand)) {
            processReload(sender);
        } else if ("set".equalsIgnoreCase(subCommand)) {
            processSet(sender, args);
        } else if ("export".equalsIgnoreCase(subCommand)) {
            processExport(sender);
        } else {
            throw new WrongUsageException(getCommandUsage(sender));
        }
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

    private void processSet(ICommandSender sender, String[] args) {
        // 用法: /ysm model set <targets> <model_id> <texture_id> [ignore_auth]
        // args[0] 是 "set"
        if (args.length < 4) {
            throw new WrongUsageException("/ysm model set <targets> <model_id> <texture_id> [ignore_auth]");
        }

        String targetSelector = args[1];
        String modelName = args[2];
        String textureName = args[3];
        boolean ignoreAuth = false;
        if (args.length >= 5) {
            ignoreAuth = parseBoolean(sender, args[4]);
        }

        List<EntityPlayerMP> targets;
        if (PlayerSelector.hasArguments(targetSelector)) {
            EntityPlayerMP[] matchedPlayers = PlayerSelector.matchPlayers(sender, targetSelector);
            if (matchedPlayers == null || matchedPlayers.length == 0) {
                targets = Collections.emptyList();
            } else {
                targets = Arrays.asList(matchedPlayers);
            }
        } else {
            EntityPlayerMP singlePlayer = getPlayer(sender, targetSelector);
            targets = Collections.singletonList(singlePlayer);
        }
        if (targets.isEmpty()) {
            throw new PlayerNotFoundException();
        }

        if (!ServerModelManager.CACHE_NAME_INFO.containsKey(modelName)) {
            sender.addChatMessage(new ChatComponentTranslation("commands.yes_steve_model.export.not_exist", modelName));
            return;
        }

        ServerModelInfo info = ServerModelManager.CACHE_NAME_INFO.get(modelName);
        if (!info.getTexture().isPresent()) {
            return;
        }

        ResourceLocation modelId = new ResourceLocation(ysmu.MODID, modelName);
        ResourceLocation textureId = ModelIdUtil.getSubModelId(modelId, textureName);

        for (EntityPlayerMP player : targets) {
            if (ignoreAuth) {
                ExtendedModelInfo eep = ExtendedModelInfo.get(player);
                if (eep != null) {
                    eep.setModelAndTexture(modelId, textureId);
                    sender.addChatMessage(new ChatComponentTranslation("message.yes_steve_model.model.set.success", modelName, player.getCommandSenderName()));
                }
            } else {
                ExtendedModelInfo eep = ExtendedModelInfo.get(player);
                if (eep != null) {
                    ExtendedAuthModels authEEP = ExtendedAuthModels.get(player);
                    if (authEEP != null) {
                        if (!ServerModelManager.AUTH_MODELS.contains(modelName) || authEEP.containModel(modelId)) {
                            eep.setModelAndTexture(modelId, textureId);
                            sender.addChatMessage(new ChatComponentTranslation("message.yes_steve_model.model.set.success", modelName, player.getCommandSenderName()));
                        } else {
                            sender.addChatMessage(new ChatComponentTranslation("message.yes_steve_model.model.set.need_auth", modelName, player.getCommandSenderName()));
                        }
                    }
                }
            }
        }
    }

    private void processExport(ICommandSender sender) {
        String infoText = GSON.toJson(ServerModelManager.CACHE_NAME_INFO);
        sender.addChatMessage(new ChatComponentText(infoText));
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
