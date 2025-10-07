package software.bernie.geckolib3.renderers.geo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.client.IItemRenderer;
import org.lwjgl.opengl.GL11;

public class RenderBlockItem implements IItemRenderer {
    protected TileEntitySpecialRenderer render;
    protected TileEntity dummytile;

    public RenderBlockItem(final TileEntitySpecialRenderer render, final TileEntity dummy) {
        this.render = render;
        this.dummytile = dummy;
    }

    public boolean handleRenderType(final ItemStack item, final IItemRenderer.ItemRenderType type) {
        return true;
    }

    public boolean shouldUseRenderHelper(final IItemRenderer.ItemRenderType type, final ItemStack item, final IItemRenderer.ItemRendererHelper helper) {
        return true;
    }

    public void renderItem(final IItemRenderer.ItemRenderType type, final ItemStack item, final Object... data) {
        if (type == IItemRenderer.ItemRenderType.ENTITY) {
            GL11.glTranslatef(-0.5f, 0.0f, -0.5f);
        }
        if (dummytile.getWorldObj() == null) {
            dummytile.setWorldObj(Minecraft.getMinecraft().theWorld);
        }
        this.render.renderTileEntityAt(this.dummytile, 0.0, 0.0, 0.0, 0.0f);
    }
}
