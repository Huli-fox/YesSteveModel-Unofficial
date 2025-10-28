package com.fox.ysmu.client;

import com.fox.ysmu.client.animation.condition.ConditionManager;
import com.fox.ysmu.client.texture.OuterFileTexture;
import com.fox.ysmu.geckolib3.core.builder.Animation;
import com.fox.ysmu.geckolib3.core.molang.MolangParser;
import com.fox.ysmu.geckolib3.file.GeckoJsonException;
import com.fox.ysmu.model.format.FolderFormat;
import com.fox.ysmu.data.ModelData;
import com.fox.ysmu.geckolib3.file.AnimationFile;
import com.fox.ysmu.geckolib3.geo.raw.pojo.ExtraInfo;
import com.fox.ysmu.geckolib3.geo.raw.pojo.FormatVersion;
import com.fox.ysmu.geckolib3.geo.raw.pojo.RawGeoModel;
import com.fox.ysmu.geckolib3.geo.raw.tree.RawGeometryTree;
import com.fox.ysmu.geckolib3.geo.render.GeoBuilder;
import com.fox.ysmu.geckolib3.geo.render.built.GeoModel;
import com.fox.ysmu.geckolib3.resource.GeckoLibCache;
import com.fox.ysmu.geckolib3.util.json.JsonAnimationUtils;
import com.fox.ysmu.model.ServerModelManager;
import com.fox.ysmu.network.NetworkHandler;
import com.fox.ysmu.network.message.SyncModelFiles;
import com.fox.ysmu.util.GsonHelper;
import com.fox.ysmu.util.ModelIdUtil;
import com.fox.ysmu.util.ObjectStreamUtil;
import com.fox.ysmu.util.ThreadTools;
import com.fox.ysmu.ysmu;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import net.minecraft.util.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

//import com.fox.ysmu.ysmu;
//import com.fox.ysmu.client.animation.condition.ConditionManager;
//import com.fox.ysmu.client.texture.OuterFileTexture;
//import com.fox.ysmu.data.ModelData;
//import com.fox.ysmu.geckolib3.core.builder.Animation;
//import com.fox.ysmu.geckolib3.core.molang.MolangParser;
//import com.fox.ysmu.geckolib3.file.AnimationFile;
//import com.fox.ysmu.geckolib3.geo.raw.pojo.ExtraInfo;
//import com.fox.ysmu.geckolib3.geo.raw.pojo.FormatVersion;
//import com.fox.ysmu.geckolib3.geo.raw.pojo.RawGeoModel;
//import com.fox.ysmu.geckolib3.geo.raw.tree.RawGeometryTree;
//import com.fox.ysmu.geckolib3.geo.render.GeoBuilder;
//import com.fox.ysmu.geckolib3.geo.render.built.GeoModel;
//import com.fox.ysmu.geckolib3.resource.GeckoLibCache;
//import com.fox.ysmu.geckolib3.util.json.JsonAnimationUtils;
//import com.fox.ysmu.model.ServerModelManager;
//import com.fox.ysmu.model.format.FolderFormat;
//import com.fox.ysmu.network.NetworkHandler;
//import com.fox.ysmu.network.message.SyncModelFiles;
//import com.fox.ysmu.util.ModelIdUtil;
//import com.fox.ysmu.util.ObjectStreamUtil;
//import com.fox.ysmu.util.ThreadTools;
import com.google.gson.JsonElement;
import it.unimi.dsi.fastutil.Pair;
//import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
//import net.minecraft.network.chat.Component;
//import net.minecraft.util.ResourceLocation;

//import net.minecraft.util.GsonHelper;
import org.apache.commons.io.filefilter.FileFileFilter;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;

public class ClientModelManager {
    public static Map<ResourceLocation, List<ResourceLocation>> MODELS = Maps.newHashMap();
    public static Map<ResourceLocation, Pair<Double, Double>> SCALE_INFO = Maps.newHashMap();
    public static Map<ResourceLocation, List<IChatComponent>> EXTRA_INFO = Maps.newHashMap();
    public static Map<ResourceLocation, String[]> EXTRA_ANIMATION_NAME = Maps.newHashMap();
    public static AnimationFile DEFAULT_ANIMATION_FILE = new AnimationFile();
    public static List<String> CACHE_MD5 = Lists.newArrayList();
    public static List<String> AUTH_MODELS = Lists.newArrayList();
    public static byte[] PASSWORD;

    public static void registerAll(ModelData data) {
        ResourceLocation modelId = new ResourceLocation(ysmu.MODID, data.getModelId());
        if (data.isAuth()) {
            AUTH_MODELS.add(data.getModelId());
        }
        ClientModelManager.registerGeo(modelId, data.getModel());
        ClientModelManager.registerAnimations(ModelIdUtil.getMainId(modelId), data.getAnimation());
        ClientModelManager.registerTexture(modelId, data.getTexture());
    }

    public static void registerGeo(ResourceLocation id, Map<String, byte[]> mapData) {
        for (String name : mapData.keySet()) {
            byte[] data = mapData.get(name);
            registerGeo(ModelIdUtil.getSubModelId(id, name), data);
        }
    }

    private static void registerGeo(ResourceLocation id, byte[] data) {
        Map<ResourceLocation, GeoModel> geoModels = GeckoLibCache.getInstance().getGeoModels();
        try {
            Object obj = ObjectStreamUtil.toObject(data);
            if (obj instanceof RawGeoModel rawModel) {
                if (rawModel.getFormatVersion() == FormatVersion.VERSION_1_12_0) {
                    // TODO 原来有个location形参，改没了，待检查
                    RawGeometryTree rawGeometryTree = RawGeometryTree.parseHierarchy(rawModel);
                    GeoModel geoModel = GeoBuilder.getGeoBuilder(id.getResourceDomain()).constructGeoModel(rawGeometryTree);
                    SCALE_INFO.put(id, Pair.of(rawGeometryTree.properties.getHeightScale(), rawGeometryTree.properties.getWidthScale()));
                    ExtraInfo extraInfo = rawGeometryTree.properties.getExtraInfo();
                    EXTRA_INFO.put(id, handleExtraInfo(id, extraInfo));
                    if (extraInfo != null && extraInfo.getExtraAnimationNames() != null && extraInfo.getExtraAnimationNames().length > 0) {
                        EXTRA_ANIMATION_NAME.put(id, extraInfo.getExtraAnimationNames());
                    }
                    geoModels.put(id, geoModel);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void registerTexture(ResourceLocation id, Map<String, byte[]> mapData) {
        List<ResourceLocation> textures = Lists.newArrayList();
        for (String name : mapData.keySet()) {
            ResourceLocation textureId = ModelIdUtil.getSubModelId(id, name);
            textures.add(textureId);
        }
        MODELS.put(id, textures);
        for (String name : mapData.keySet()) {
            byte[] data = mapData.get(name);
            ResourceLocation textureId = ModelIdUtil.getSubModelId(id, name);
            registerTexture(textureId, data);
        }
    }

    private static void registerTexture(ResourceLocation id, byte[] data) {
        Minecraft.getMinecraft().getTextureManager().loadTexture(id, new OuterFileTexture(data));
    }

    private static void registerAnimations(ResourceLocation id, Map<String, byte[]> mapData) {
        Map<ResourceLocation, AnimationFile> animations = GeckoLibCache.getInstance().getAnimations();
        AnimationFile main = new AnimationFile();
        mapData.forEach((name, bytes) -> {
            AnimationFile other = getAnimationFile(new String(bytes, StandardCharsets.UTF_8));
            mergeAnimationFile(main, other);
        });
        DEFAULT_ANIMATION_FILE.animations().forEach((name, action) -> {
            if (!main.animations().containsKey(name)) {
                main.putAnimation(name, action);
            }
        });
        main.animations().forEach((name, animation) -> ConditionManager.addTest(id, name));
        animations.put(id, main);
    }

    private static AnimationFile getAnimationFile(String file) {
        AnimationFile animationFile = new AnimationFile();
        MolangParser parser = GeckoLibCache.getInstance().parser;
        JsonObject jsonObject = GsonHelper.fromJson(ysmu.GSON, file, JsonObject.class);
        if (jsonObject != null) {
            for (Map.Entry<String, JsonElement> entry : JsonAnimationUtils.getAnimations(jsonObject)) {
                String animationName = entry.getKey();
                Animation animation;
                try {
                    animation = JsonAnimationUtils.deserializeJsonToAnimation(JsonAnimationUtils.getAnimation(jsonObject, animationName), parser);
                    animationFile.putAnimation(animationName, animation);
                } catch (GeckoJsonException e) {
                    e.printStackTrace();
                }
            }
        }
        return animationFile;
    }

    private static AnimationFile mergeAnimationFile(AnimationFile main, AnimationFile other) {
        other.animations().forEach(main::putAnimation);
        return main;
    }

    public static void loadDefaultModel() {
        try {
            ModelData data = FolderFormat.getModelData(ServerModelManager.CUSTOM, "default", false);
            data.getAnimation().forEach((name, bytes) -> {
                AnimationFile animationFile = getAnimationFile(new String(bytes, StandardCharsets.UTF_8));
                mergeAnimationFile(DEFAULT_ANIMATION_FILE, animationFile);
            });
            ClientModelManager.registerAll(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendSyncModelMessage() {
        MODELS.clear();
        CACHE_MD5.clear();
        AUTH_MODELS.clear();
        SCALE_INFO.clear();
        EXTRA_INFO.clear();
        EXTRA_ANIMATION_NAME.clear();
        ConditionManager.clear();
        String[] md5Info = getMd5Info();
        SyncModelFiles syncModelFiles = new SyncModelFiles(md5Info);
        ThreadTools.THREAD_POOL.submit(() -> {
            try {
                while (Minecraft.getMinecraft().theWorld == null) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                NetworkHandler.CHANNEL.sendToServer(syncModelFiles);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    private static String[] getMd5Info() {
        Collection<File> files = FileUtils.listFiles(ServerModelManager.CACHE_CLIENT.toFile(), FileFileFilter.FILE, null);
        String[] output = new String[files.size()];
        int i = 0;
        for (File file : files) {
            output[i] = file.getName();
            i++;
        }
        return output;
    }

    private static byte[] getBytes(Path root, String fileName) throws IOException {
        return FileUtils.readFileToByteArray(root.resolve(fileName).toFile());
    }

    @Nullable
    private static List<IChatComponent> handleExtraInfo(ResourceLocation id, @Nullable ExtraInfo extraInfo) {
        if (extraInfo == null || StringUtils.isBlank(extraInfo.getName())) {
            return null;
        }
        List<IChatComponent> component = Lists.newArrayList();
        IChatComponent textComponent = new ChatComponentText(extraInfo.getName());
        textComponent.getChatStyle().setColor(EnumChatFormatting.GOLD);
        component.add(textComponent);
        if (StringUtils.isNoneBlank(extraInfo.getTips())) {
            String[] split = extraInfo.getTips().split("\n");
            for (String s : split) {
                IChatComponent lineComponent = new ChatComponentText(s);
                lineComponent.getChatStyle().setColor(EnumChatFormatting.GRAY);
                component.add(lineComponent);
            }
        }
        if (extraInfo.getAuthors() != null && extraInfo.getAuthors().length != 0) {
            component.add(new ChatComponentTranslation("gui.yes_steve_model.model.authors", StringUtils.join(extraInfo.getAuthors(), "丨")));
        }
        if (StringUtils.isNoneBlank(extraInfo.getLicense())) {
            component.add(new ChatComponentTranslation("gui.yes_steve_model.model.license", extraInfo.getLicense()));
        }
        return component;
    }
}
