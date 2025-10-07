package software.bernie.example.client.model.tile;

import net.minecraft.util.ResourceLocation;
import software.bernie.example.block.tile.DiagonalTileEntity;
import software.bernie.geckolib3.GeckoLib;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class DiagonalModel extends AnimatedGeoModel<DiagonalTileEntity> {
    @Override
    public ResourceLocation getAnimationFileLocation(DiagonalTileEntity entity) {
        return new ResourceLocation(GeckoLib.ModID, "animations/bat.animation.json");
    }

    @Override
    public ResourceLocation getModelLocation(DiagonalTileEntity animatable) {
        return new ResourceLocation(GeckoLib.ModID, "geo/testdiagonal.geo.json");
    }

    @Override
    public ResourceLocation getTextureLocation(DiagonalTileEntity entity) {
        return new ResourceLocation(GeckoLib.ModID, "textures/block/testdiagonal.png");
    }
}
