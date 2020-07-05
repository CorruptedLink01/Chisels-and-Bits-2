package nl.dgoossens.chiselsandbits2.common.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import nl.dgoossens.chiselsandbits2.api.bit.BitLocation;
import nl.dgoossens.chiselsandbits2.api.item.attributes.VoxelStorer;
import nl.dgoossens.chiselsandbits2.common.blocks.ChiseledBlockTileEntity;
import nl.dgoossens.chiselsandbits2.api.voxel.ExtendedVoxelBlob;
import nl.dgoossens.chiselsandbits2.api.voxel.IntegerBox;
import nl.dgoossens.chiselsandbits2.api.voxel.VoxelBlob;
import nl.dgoossens.chiselsandbits2.common.impl.item.PlayerItemMode;

import java.util.function.Supplier;

/**
 * A class dedicated to calculating whether or not we can currently place a block and where it gets placed
 * if we can.
 */
public class BlockPlacementLogic {
    /**
     * Returns whether or not a block is placeable.
     */
    public static boolean isNotPlaceable(final PlayerEntity player, final World world, final BlockPos pos, final Direction face, final PlayerItemMode mode, final Supplier<VoxelBlob> nbtSupplier) {
        if (ChiselUtil.isBlockReplaceable(world, pos, player, face, false))
            return false;

        if (mode.equals(PlayerItemMode.CHISELED_BLOCK_FIT)) {
            if (world.getTileEntity(pos) instanceof ChiseledBlockTileEntity) {
                ChiseledBlockTileEntity cbte = (ChiseledBlockTileEntity) world.getTileEntity(pos);
                if (cbte != null && !nbtSupplier.get().canMerge(cbte.getVoxelBlob()))
                    return true; //Can't place if we can't merge this
                return false;
            }
        }
        switch (mode) {
            case CHISELED_BLOCK_GRID:
                return true;
            default:
                return !(world.getTileEntity(pos) instanceof ChiseledBlockTileEntity);
        }
    }

    /**
     * Returns whether or not a block is placeable when placing off-grid.
     */
    public static boolean isNotPlaceableOffGrid(final PlayerEntity player, final World world, final Direction face, final BitLocation target, final ItemStack item) {
        if (!(item.getItem() instanceof VoxelStorer)) return true;
        VoxelStorer it = (VoxelStorer) item.getItem();

        final ExtendedVoxelBlob evb = new ExtendedVoxelBlob(3, 3, 3, -1, -1, -1);
        final VoxelBlob placedBlob = it.getVoxelBlob(item);
        evb.insertBlob(0, 0, 0, placedBlob);
        final IntegerBox bounds = placedBlob.getBounds();
        final BlockPos offset = BlockPlacementLogic.getPartialOffset(face, new BlockPos(target.bitX, target.bitY, target.bitZ), bounds);
        evb.shift(offset.getX(), offset.getY(), offset.getZ());

        for (BlockPos pos : evb.listBlocks()) {
            pos = pos.add(target.blockPos);
            //If we can't chisel here, don't chisel.
            if (world.getServer() != null && world.getServer().isBlockProtected(world, pos, player))
                return true;

            if (!ChiselUtil.canChiselPosition(pos, player, world.getBlockState(pos), face))
                return true;

            if (!ChiselUtil.isBlockChiselable(world, pos, player, world.getBlockState(pos), face))
                return true;
        }
        return false;
    }

    /**
     * Calculates the partial offset used by ghost rendering.
     */
    public static BlockPos getPartialOffset(final Direction side, final BlockPos partial, final IntegerBox modelBounds) {
        int offset_x = modelBounds.minX;
        int offset_y = modelBounds.minY;
        int offset_z = modelBounds.minZ;

        final int partial_x = partial.getX();
        final int partial_y = partial.getY();
        final int partial_z = partial.getZ();

        int middle_x = (modelBounds.maxX - modelBounds.minX) / -2;
        int middle_y = (modelBounds.maxY - modelBounds.minY) / -2;
        int middle_z = (modelBounds.maxZ - modelBounds.minZ) / -2;

        switch (side) {
            case DOWN:
                offset_y = modelBounds.maxY;
                middle_y = 0;
                break;
            case EAST:
                offset_x = modelBounds.minX;
                middle_x = 0;
                break;
            case NORTH:
                offset_z = modelBounds.maxZ;
                middle_z = 0;
                break;
            case SOUTH:
                offset_z = modelBounds.minZ;
                middle_z = 0;
                break;
            case UP:
                offset_y = modelBounds.minY;
                middle_y = 0;
                break;
            case WEST:
                offset_x = modelBounds.maxX;
                middle_x = 0;
                break;
            default:
                throw new NullPointerException();
        }

        final int t_x = -offset_x + middle_x + partial_x;
        final int t_y = -offset_y + middle_y + partial_y;
        final int t_z = -offset_z + middle_z + partial_z;

        return new BlockPos(t_x, t_y, t_z);
    }
}