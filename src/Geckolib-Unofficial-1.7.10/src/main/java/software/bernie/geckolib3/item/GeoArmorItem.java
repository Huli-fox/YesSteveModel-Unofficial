package software.bernie.geckolib3.item;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import software.bernie.geckolib3.renderers.geo.GeoArmorRenderer;

import javax.annotation.Nullable;

@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class GeoArmorItem extends ItemArmor {
    public GeoArmorItem(ArmorMaterial materialIn, int renderIndexIn, int slot) {
        super(materialIn, renderIndexIn, slot);
    }

    @Nullable
    @SideOnly(Side.CLIENT)
    @Override
    public ModelBiped getArmorModel(EntityLivingBase entityLiving, ItemStack itemStack, int armorSlot) {
        Class<? extends ItemArmor> clazz = this.getClass();
        GeoArmorRenderer renderer = GeoArmorRenderer.getRenderer(clazz);
        renderer.setCurrentItem(entityLiving, itemStack, armorSlot);
        renderer.applyEntityStats(entityLiving).applySlot(armorSlot);
        return renderer;
    }

    @Nullable
    @SideOnly(Side.CLIENT)
    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, int slot, String type) {
        Class<? extends ItemArmor> clazz = this.getClass();
        GeoArmorRenderer renderer = GeoArmorRenderer.getRenderer(clazz);
        return renderer.getTextureLocation((ItemArmor) stack.getItem()).toString();
    }
}
