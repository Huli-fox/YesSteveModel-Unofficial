package software.bernie.example.client.renderer.tile;

import software.bernie.example.block.tile.DiagonalTileEntity;
import software.bernie.example.client.model.tile.DiagonalModel;
import software.bernie.geckolib3.collision.ComplexBB;
import software.bernie.geckolib3.geo.render.built.GeoModel;
import software.bernie.geckolib3.renderers.geo.GeoBlockRenderer;

public class DiagonalTileRenderer extends GeoBlockRenderer<DiagonalTileEntity> {
    public DiagonalTileRenderer() {
        super(new DiagonalModel());
    }

    @Override
    public void renderEarly(GeoModel model, DiagonalTileEntity animatable, float ticks, float red, float green, float blue, float alpha) {
        super.renderEarly(model, animatable, ticks, red, green, blue, alpha);
//        Cube cube = new Cube();
//        cube.setUv(new UvUnion(0,0));
//        MatrixStack stack = new MatrixStack();
//        animatable.boundingBox = new RotatableBB(GeoCube.createFromPojoCube(cube, model.properties, null,null),stack);//new ComplexBB(model, animatable.xCoord,animatable.yCoord,animatable.zCoord);
        animatable.boundingBox = new ComplexBB(model, animatable.xCoord + 0.5, animatable.yCoord, animatable.zCoord + 0.5);
    }
}
