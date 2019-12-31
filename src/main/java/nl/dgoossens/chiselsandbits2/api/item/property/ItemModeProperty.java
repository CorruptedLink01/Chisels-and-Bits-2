package nl.dgoossens.chiselsandbits2.api.item.property;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.ByteNBT;
import net.minecraft.nbt.IntNBT;
import nl.dgoossens.chiselsandbits2.api.item.IItemMode;
import nl.dgoossens.chiselsandbits2.api.item.IItemModeType;
import nl.dgoossens.chiselsandbits2.common.impl.ItemMode;

public class ItemModeProperty extends IItemProperty<IItemMode> {
    private  final IItemModeType type;
    public ItemModeProperty(final IItemModeType type) {
        this.type = type;
    }

    @Override
    public IItemMode get(final ItemStack stack) {
        if(stack.hasTag() && stack.getTag().contains("bmode_"+slot)) {
            boolean b = stack.getTag().getBoolean("bmode_"+slot);
            if(b) {
                return ItemMode.values()[stack.getTag().getInt("mode_"+slot)];
            } else
                throw new UnsupportedOperationException("No support for custom item mode properties yet!");
        }
        return type.getDefault();
    }

    @Override
    public void set(ItemStack stack, IItemMode value) {
        super.set(stack, value);
        if(value instanceof ItemMode) {
            stack.setTagInfo("bmode_"+slot, new ByteNBT((byte) 1));
            stack.setTagInfo("mode_"+slot, new IntNBT(((ItemMode) value).ordinal()));
        } else
            stack.setTagInfo("bmode_"+slot, new ByteNBT((byte) 0));
    }
}