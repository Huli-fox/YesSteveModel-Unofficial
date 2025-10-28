package com.fox.ysmu.client.animation.condition;

import com.google.common.collect.Lists;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITagManager;

import java.util.List;

public class ConditionalSwing {
    private static final String ID_PRE = "swing$";
    private static final String TAG_PRE = "swing#";
    private static final String EMPTY = "";
    private static final int PRE_SIZE = 6;
    private final List<ResourceLocation> idTest = Lists.newArrayList();
    private final List<TagKey<Item>> tagTest = Lists.newArrayList();

    public void addTest(String name) {
        if (name.length() <= PRE_SIZE) {
            return;
        }
        String substring = name.substring(PRE_SIZE);
        if (name.startsWith(ID_PRE) && ResourceLocation.isValidResourceLocation(substring)) {
            idTest.add(new ResourceLocation(name.substring(PRE_SIZE)));
        }
        if (name.startsWith(TAG_PRE) && ResourceLocation.isValidResourceLocation(substring)) {
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
            return ID_PRE + registryName;
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
        return tagTest.stream().filter(itemInHand::is).findFirst().map(itemTagKey -> TAG_PRE + itemTagKey.location()).orElse(EMPTY);
    }
}
