package software.bernie.example;

import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import software.bernie.example.block.BotariumBlock;
import software.bernie.example.block.DiagonalBlock;
import software.bernie.example.block.FertilizerBlock;
import software.bernie.example.block.tile.BotariumTileEntity;
import software.bernie.example.block.tile.FertilizerTileEntity;
import software.bernie.example.entity.BikeEntity;
import software.bernie.example.entity.GeoExampleEntity;
import software.bernie.example.entity.GeoExampleEntityLayer;
import software.bernie.example.entity.GeoNpcEntity;
import software.bernie.example.item.JackInTheBoxItem;
import software.bernie.example.item.PotatoArmorItem;
import software.bernie.example.registry.BlockRegistry;
import software.bernie.example.registry.ItemRegistry;

public class CommonListener {

    public static void onRegisterBlocks() {
        BlockRegistry.BOTARIUM_BLOCK = new BotariumBlock();
        BlockRegistry.FERTILIZER_BLOCK = new FertilizerBlock();
        BlockRegistry.DIAGONAL_BLOCK = new DiagonalBlock();

        BlockRegistry.BOTARIUM_BLOCK.setCreativeTab(GeckoLibMod.getGeckolibItemGroup());
        BlockRegistry.FERTILIZER_BLOCK.setCreativeTab(GeckoLibMod.getGeckolibItemGroup());
        BlockRegistry.DIAGONAL_BLOCK.setCreativeTab(GeckoLibMod.getGeckolibItemGroup());

        registerBlock(BlockRegistry.BOTARIUM_BLOCK, "botariumblock");
        registerBlock(BlockRegistry.FERTILIZER_BLOCK, "fertilizerblock");
        //registerBlock(BlockRegistry.DIAGONAL_BLOCK,"diagonalblock");
    }

    public static void onRegisterEntities() {
        int id = 0;
        EntityRegistry.registerModEntity(BikeEntity.class, "bike", id++, GeckoLibMod.instance, 160, 2, false);
        EntityRegistry.registerModEntity(GeoExampleEntity.class, "example", id++, GeckoLibMod.instance, 160, 2, false);
        EntityRegistry.registerModEntity(GeoExampleEntityLayer.class, "examplelayer", id++, GeckoLibMod.instance, 160, 2, false);
        EntityRegistry.registerModEntity(GeoNpcEntity.class, "geonpc", id++, GeckoLibMod.instance, 160, 2, false);
        /* Tile entities */
        GameRegistry.registerTileEntity(BotariumTileEntity.class, "botariumtile");
        GameRegistry.registerTileEntity(FertilizerTileEntity.class, "fertilizertile");
        //GameRegistry.registerTileEntity(DiagonalTileEntity.class, "diagonaltile");
    }

    public static void onRegisterItems() {
        ItemRegistry.JACK_IN_THE_BOX = new JackInTheBoxItem();
        ItemRegistry.JACK_IN_THE_BOX.setUnlocalizedName("jackintheboxitem");
        GameRegistry.registerItem(ItemRegistry.JACK_IN_THE_BOX, "jackintheboxitem");

        ItemRegistry.POTATO_HEAD = registerItem(
            new PotatoArmorItem(ItemArmor.ArmorMaterial.DIAMOND, 0, 0), "potato_head");
        ItemRegistry.POTATO_CHEST = registerItem(
            new PotatoArmorItem(ItemArmor.ArmorMaterial.DIAMOND, 0, 1), "potato_chest");
        ItemRegistry.POTATO_LEGGINGS = registerItem(
            new PotatoArmorItem(ItemArmor.ArmorMaterial.DIAMOND, 0, 2), "potato_leggings");
        ItemRegistry.POTATO_BOOTS = registerItem(
            new PotatoArmorItem(ItemArmor.ArmorMaterial.DIAMOND, 0, 3), "potato_boots");
    }

    private static <T extends Item> T registerItem(T item, String name) {
        GameRegistry.registerItem(item.setUnlocalizedName(name).setTextureName("geckolib3:" + name), name);
        return item;
    }

    private static void registerBlock(Block block, String name) {
        GameRegistry.registerBlock(block.setBlockName(name), name);
    }
}
