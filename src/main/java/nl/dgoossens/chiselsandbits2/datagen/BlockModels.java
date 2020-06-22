package nl.dgoossens.chiselsandbits2.datagen;

import net.minecraft.block.Block;
import net.minecraft.data.DataGenerator;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.generators.BlockModelProvider;
import net.minecraftforge.client.model.generators.ExistingFileHelper;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelBuilder;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;

public class BlockModels extends BlockModelProvider {
    public BlockModels(DataGenerator dataGenerator, ExistingFileHelper existingFileHandler) {
        super(dataGenerator, ChiselsAndBits2.MOD_ID, existingFileHandler);
    }

    @Override
    public String getName() {
        return "Chisels & Bits 2: Block Models";
    }

    @Override
    protected void registerModels() {
        Block chiseledBlock =  ChiselsAndBits2.getInstance().getRegister().CHISELED_BLOCK.get();
        getBuilder(chiseledBlock.getRegistryName().getPath());
    }
}