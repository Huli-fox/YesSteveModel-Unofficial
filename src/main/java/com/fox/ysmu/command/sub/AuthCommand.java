package com.fox.ysmu.command.sub;

import java.util.Arrays;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerSelector;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ResourceLocation;

import com.fox.ysmu.eep.ExtendedAuthModels;
import com.fox.ysmu.eep.ExtendedModelInfo;
import com.fox.ysmu.model.ServerModelManager;
import com.fox.ysmu.network.NetworkHandler;
import com.fox.ysmu.network.message.SyncAuthModels;
import com.fox.ysmu.ysmu;

public class AuthCommand extends CommandBase {

    private static final String AUTH_NAME = "auth";

    @Override
    public String getCommandName() {
        return AUTH_NAME;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/" + AUTH_NAME + " <targets> <add|remove|all|clear> [model_id]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.addChatMessage(new ChatComponentTranslation("commands.yes_steve_model.auth.usage"));
            return;
        }

        String targetSelector = args[0];
        String action = args[1];

        try {
            List<EntityPlayerMP> targets = Arrays.asList(PlayerSelector.matchPlayers(sender, targetSelector));
            if (targets.isEmpty()) {
                sender.addChatMessage(new ChatComponentTranslation("commands.yes_steve_model.auth.no_targets"));
                return;
            }

            switch (action) {
                case "add":
                    if (args.length < 3) {
                        sender.addChatMessage(new ChatComponentTranslation("commands.yes_steve_model.auth.add.usage"));
                        return;
                    }
                    addAuthModel(sender, targets, args[2]);
                    break;
                case "remove":
                    if (args.length < 3) {
                        sender
                            .addChatMessage(new ChatComponentTranslation("commands.yes_steve_model.auth.remove.usage"));
                        return;
                    }
                    removeAuthModel(sender, targets, args[2]);
                    break;
                case "all":
                    addAllAuthModel(sender, targets);
                    break;
                case "clear":
                    clearAuthModel(sender, targets);
                    break;
                default:
                    sender.addChatMessage(new ChatComponentTranslation("commands.yes_steve_model.auth.invalid_action"));
                    break;
            }
        } catch (Exception e) {
            sender.addChatMessage(new ChatComponentTranslation("commands.yes_steve_model.auth.error", e.getMessage()));
        }
    }

    private void addAuthModel(ICommandSender sender, List<EntityPlayerMP> targets, String modelName) {
        if (!ServerModelManager.CACHE_NAME_INFO.containsKey(modelName)) {
            sender.addChatMessage(new ChatComponentTranslation("commands.yes_steve_model.export.not_exist", modelName));
            return;
        }

        for (EntityPlayerMP player : targets) {
            ExtendedAuthModels eep = ExtendedAuthModels.get(player);
            if (eep != null) {
                ResourceLocation modelId = new ResourceLocation(ysmu.MODID, modelName);
                eep.addModel(modelId);
                NetworkHandler.sendToClientPlayer(new SyncAuthModels(eep.getAuthModels()), player);
                sender.addChatMessage(
                    new ChatComponentTranslation(
                        "commands.yes_steve_model.auth_model.add.info",
                        modelId.getResourcePath(),
                        player.getCommandSenderName()));
            }
        }
    }

    private void addAllAuthModel(ICommandSender sender, List<EntityPlayerMP> targets) {
        for (EntityPlayerMP player : targets) {
            ExtendedAuthModels eep = ExtendedAuthModels.get(player);
            if (eep != null) {
                ServerModelManager.CACHE_NAME_INFO.keySet()
                    .forEach(name -> eep.addModel(new ResourceLocation(ysmu.MODID, name)));
                NetworkHandler.sendToClientPlayer(new SyncAuthModels(eep.getAuthModels()), player);
                sender.addChatMessage(
                    new ChatComponentTranslation(
                        "commands.yes_steve_model.auth_model.all.info",
                        player.getCommandSenderName()));
            }
        }
    }

    private void removeAuthModel(ICommandSender sender, List<EntityPlayerMP> targets, String modelName) {
        ResourceLocation modelId = new ResourceLocation(ysmu.MODID, modelName);
        for (EntityPlayerMP player : targets) {
            ExtendedAuthModels ownModelsEEP = ExtendedAuthModels.get(player);
            if (ownModelsEEP != null) {
                ownModelsEEP.removeModel(modelId);
                ExtendedModelInfo modelIdEEP = ExtendedModelInfo.get(player);
                if (modelIdEEP != null) {
                    if (ServerModelManager.AUTH_MODELS.contains(
                        modelIdEEP.getModelId()
                            .getResourcePath())
                        && !ownModelsEEP.containModel(modelIdEEP.getModelId())) {
                        ResourceLocation defaultModelId = new ResourceLocation(ysmu.MODID, "default");
                        ResourceLocation defaultTextureId = new ResourceLocation(ysmu.MODID, "default/default.png");
                        modelIdEEP.setModelAndTexture(defaultModelId, defaultTextureId);
                    }
                }
                NetworkHandler.sendToClientPlayer(new SyncAuthModels(ownModelsEEP.getAuthModels()), player);
                sender.addChatMessage(
                    new ChatComponentTranslation(
                        "commands.yes_steve_model.auth_model.remove.info",
                        modelId.getResourcePath(),
                        player.getCommandSenderName()));
            }
        }
    }

    private void clearAuthModel(ICommandSender sender, List<EntityPlayerMP> targets) {
        for (EntityPlayerMP player : targets) {
            ExtendedAuthModels ownModelEEP = ExtendedAuthModels.get(player);
            if (ownModelEEP != null) {
                ownModelEEP.clear();
                ExtendedModelInfo modelIdEEP = ExtendedModelInfo.get(player);
                if (modelIdEEP != null) {
                    ResourceLocation defaultModelId = new ResourceLocation(ysmu.MODID, "default");
                    ResourceLocation defaultTextureId = new ResourceLocation(ysmu.MODID, "default/default.png");
                    modelIdEEP.setModelAndTexture(defaultModelId, defaultTextureId);
                }
                NetworkHandler.sendToClientPlayer(new SyncAuthModels(ownModelEEP.getAuthModels()), player);
                sender.addChatMessage(
                    new ChatComponentTranslation(
                        "commands.yes_steve_model.auth_model.clear.info",
                        player.getCommandSenderName()));
            }
        }
    }

    @Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(
                args,
                MinecraftServer.getServer()
                    .getAllUsernames());
        } else if (args.length == 2) {
            List<String> actions = java.util.Arrays.asList("add", "remove", "all", "clear");
            return getListOfStringsMatchingLastWord(args, actions.toArray(new String[0]));
        }
        return null;
    }
}
