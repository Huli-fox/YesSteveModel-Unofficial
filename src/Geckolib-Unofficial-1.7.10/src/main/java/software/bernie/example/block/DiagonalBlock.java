package software.bernie.example.block;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import software.bernie.example.GeckoLibMod;
import software.bernie.example.block.tile.DiagonalTileEntity;

import javax.annotation.Nullable;
import java.util.List;

public class DiagonalBlock extends Block implements ITileEntityProvider {
    public AxisAlignedBB boundingBox;

    public DiagonalBlock() {
        super(Material.rock);
        this.setCreativeTab(GeckoLibMod.getGeckolibItemGroup());
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World p_149668_1_, int p_149668_2_, int p_149668_3_, int p_149668_4_) {
        setBlockBoundsBasedOnState(p_149668_1_, p_149668_2_, p_149668_3_, p_149668_4_);
        if (boundingBox == null) {
            return super.getCollisionBoundingBoxFromPool(p_149668_1_, p_149668_2_, p_149668_3_, p_149668_4_);
        }
        return boundingBox;
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess p_149719_1_, int p_149719_2_, int p_149719_3_, int p_149719_4_) {
        if (p_149719_1_.getTileEntity(p_149719_2_, p_149719_3_, p_149719_4_) != null) {
            boundingBox = ((DiagonalTileEntity) p_149719_1_.getTileEntity(p_149719_2_, p_149719_3_, p_149719_4_)).boundingBox;
        } else {
            boundingBox = AxisAlignedBB.getBoundingBox((double) p_149719_2_ + this.minX, (double) p_149719_3_ + this.minY, (double) p_149719_4_ + this.minZ, (double) p_149719_2_ + this.maxX, (double) p_149719_3_ + this.maxY, (double) p_149719_4_ + this.maxZ);
        }
    }

    @Override
    public AxisAlignedBB getSelectedBoundingBoxFromPool(World p_149633_1_, int p_149633_2_, int p_149633_3_, int p_149633_4_) {
        setBlockBoundsBasedOnState(p_149633_1_, p_149633_2_, p_149633_3_, p_149633_4_);
        if (boundingBox == null) {
            return super.getSelectedBoundingBoxFromPool(p_149633_1_, p_149633_2_, p_149633_3_, p_149633_4_);
        }
        return boundingBox;
    }

    @Override
    public void addCollisionBoxesToList(World p_149743_1_, int p_149743_2_, int p_149743_3_, int p_149743_4_, AxisAlignedBB p_149743_5_, List p_149743_6_, Entity p_149743_7_) {
        if (boundingBox == null) {
            super.addCollisionBoxesToList(p_149743_1_, p_149743_2_, p_149743_3_, p_149743_4_, p_149743_5_, p_149743_6_, p_149743_7_);
        } else {
            if (boundingBox.intersectsWith(p_149743_5_)) {
                p_149743_6_.add(boundingBox);
            }
        }
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public int getRenderType() {
        return -1;
    }

    @Override
    public boolean hasTileEntity() {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new DiagonalTileEntity();
    }
}
