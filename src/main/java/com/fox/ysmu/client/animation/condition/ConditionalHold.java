package com.fox.ysmu.client.animation.condition;

import com.google.common.collect.Lists;
import net.minecraft.util.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITagManager;

import java.util.List;

public class ConditionalHold {
    private static final String EMPTY = "";
    private final int preSize;
    private final String idPre;
    private final String tagPre;
    private final List<ResourceLocation> idTest = Lists.newArrayList();
    private final List<TagKey<Item>> tagTest = Lists.newArrayList();

    public ConditionalHold(InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND) {
            idPre = "hold_mainhand$";
            tagPre = "hold_mainhand#";
            preSize = 14;
        } else {
            idPre = "hold_offhand$";
            tagPre = "hold_offhand#";
            preSize = 13;
        }
    }

    public void addTest(String name) {
        if (name.length() <= preSize) {
            return;
        }
        String substring = name.substring(preSize);
        if (name.startsWith(idPre) && ResourceLocation.isValidResourceLocation(substring)) {
            idTest.add(new ResourceLocation(substring));
        }
        if (name.startsWith(tagPre) && ResourceLocation.isValidResourceLocation(substring)) {
            ITagManager<Item> tags = ForgeRegistries.ITEMS.tags();
            if (tags == null) {
                return;
            }
            TagKey<Item> tagKey = tags.createTagKey(new ResourceLocation(substring));
            tagTest.add(tagKey);
        }
    }

    public String doTest(Player player, InteractionHand hand) {
        if (player.getItemInHand(hand).isEmpty()) {
            return EMPTY;
        }
        String result = doIdTest(player, hand);
        if (result.isEmpty()) {
            return doTagTest(player, hand);
        }
        return result;
    }

    private String doIdTest(Player player, InteractionHand hand) {
        if (idTest.isEmpty()) {
            return EMPTY;
        }
        ItemStack itemInHand = player.getItemInHand(hand);
        ResourceLocation registryName = ForgeRegistries.ITEMS.getKey(itemInHand.getItem());
        if (registryName == null) {
            return EMPTY;
        }
        if (idTest.contains(registryName)) {
            return idPre + registryName;
        }
        return EMPTY;
    }

    private String doTagTest(Player player, InteractionHand hand) {
        if (tagTest.isEmpty()) {
            return EMPTY;
        }
        ItemStack itemInHand = player.getItemInHand(hand);
        ITagManager<Item> tags = ForgeRegistries.ITEMS.tags();
        if (tags == null) {
            return EMPTY;
        }
        return tagTest.stream().filter(itemInHand::is).findFirst().map(itemTagKey -> tagPre + itemTagKey.location()).orElse(EMPTY);
    }
}
