package com.fox.ysmu.client.event;

import com.fox.ysmu.ysmu;
import com.fox.ysmu.client.ClientModelManager;
import com.fox.ysmu.network.message.RequestLoadModel;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD, modid = ysmu.MODID)
public class ReloadResourceEvent {
    public static final ResourceLocation BLOCK_ATLAS_TEXTURE = new ResourceLocation("textures/atlas/blocks.png");
    public static int DEBUG_BG_WIDTH = 1000;
    private static final Pattern INT_REG = Pattern.compile("^[0-9]*$");

    @SubscribeEvent
    public static void onTextureStitchEventPost(TextureStitchEvent.Post event) {
        if (BLOCK_ATLAS_TEXTURE.equals(event.getAtlas().location())) {
            ClientModelManager.loadDefaultModel();
            ClientModelManager.CACHE_MD5.forEach(RequestLoadModel::loadModel);
            Matcher matcher = INT_REG.matcher(I18n.get("molang.yes_steve_model.bg_width"));
            if (matcher.matches()) {
                DEBUG_BG_WIDTH = Integer.parseInt(I18n.get("molang.yes_steve_model.bg_width"));
            }
        }
    }
}
