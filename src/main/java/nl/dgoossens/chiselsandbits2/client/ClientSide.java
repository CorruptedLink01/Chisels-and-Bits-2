package nl.dgoossens.chiselsandbits2.client;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.api.bit.VoxelWrapper;
import nl.dgoossens.chiselsandbits2.api.cache.CacheType;
import nl.dgoossens.chiselsandbits2.api.item.*;
import nl.dgoossens.chiselsandbits2.api.bit.VoxelType;
import nl.dgoossens.chiselsandbits2.api.item.IMenuAction;
import nl.dgoossens.chiselsandbits2.api.item.attributes.IItemScrollWheel;
import nl.dgoossens.chiselsandbits2.client.render.chiseledblock.model.ChiseledBlockSmartModel;
import nl.dgoossens.chiselsandbits2.client.render.chiseledblock.ter.GfxRenderState;
import nl.dgoossens.chiselsandbits2.client.render.color.ColourableItemColor;
import nl.dgoossens.chiselsandbits2.client.render.color.ChiseledBlockColor;
import nl.dgoossens.chiselsandbits2.client.render.color.ChiseledBlockItemColor;
import nl.dgoossens.chiselsandbits2.client.render.chiseledblock.ter.ChiseledBlockTER;
import nl.dgoossens.chiselsandbits2.client.render.morphingbit.MorphingBitSmartModel;
import nl.dgoossens.chiselsandbits2.common.blocks.ChiseledBlockTileEntity;
import nl.dgoossens.chiselsandbits2.common.impl.item.ItemMode;
import nl.dgoossens.chiselsandbits2.common.impl.item.MenuAction;
import nl.dgoossens.chiselsandbits2.common.items.ChiselMimicItem;
import nl.dgoossens.chiselsandbits2.common.items.StorageItem;
import nl.dgoossens.chiselsandbits2.common.items.TypedItem;
import nl.dgoossens.chiselsandbits2.common.util.ItemPropertyUtil;
import nl.dgoossens.chiselsandbits2.common.registry.ModKeybindings;

import java.lang.reflect.Field;

/**
 * Handles all features triggered by client-sided events.
 * Examples:
 * - Block Highlights
 * - Placement Ghost
 * - Tape Measure
 * - Item Scrolling
 *
 * Events are located in this class, all methods are put in ClientSideHelper.
 */
@Mod.EventBusSubscriber(Dist.CLIENT)
public class ClientSide extends ClientSideHelper {
    //--- GENERAL SETUP ---
    /**
     * Setup all client side only things to register.
     */
    public void setup() {
        ClientRegistry.bindTileEntitySpecialRenderer(ChiseledBlockTileEntity.class, new ChiseledBlockTER());

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::registerItemColors);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::registerBlockColors);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::registerIconTextures);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clearCaches);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::bakeModels);
    }

    /**
     * Register item color handlers.
     */
    public void registerItemColors(final ColorHandlerEvent.Item e) {
        e.getItemColors().register(new ChiseledBlockItemColor(),
                ChiselsAndBits2.getInstance().getRegister().CHISELED_BLOCK_ITEM.get(),
                ChiselsAndBits2.getInstance().getRegister().MORPHING_BIT.get(),
                ChiselsAndBits2.getInstance().getRegister().);
    }

    /**
     * Register block color handlers.
     */
    public void registerBlockColors(final ColorHandlerEvent.Block e) {
        e.getBlockColors().register(new ChiseledBlockColor(), ChiselsAndBits2.getInstance().getRegister().CHISELED_BLOCK.get());
    }

    /**
     * Register custom sprites.
     */
    public void registerIconTextures(final TextureStitchEvent.Pre e) {
        //Only register to the texture map.
        if(!e.getMap().getBasePath().equals("textures")) return;

        //We only do this for our own, addons need to do this themselves.
        for (final IMenuAction menuAction : MenuAction.values()) {
            if (!menuAction.hasIcon()) continue;
            menuActionLocations.put(menuAction, menuAction.getIconResourceLocation());
            e.addSprite(menuActionLocations.get(menuAction));
        }

        for (final ItemModeEnum itemMode : ItemMode.values()) {
            if (!itemMode.hasIcon()) continue;
            modeIconLocations.put(itemMode, itemMode.getIconResourceLocation());
            e.addSprite(modeIconLocations.get(itemMode));
        }
    }

    /**
     * Clear the cached model data whenever textures are stitched.
     */
    public void clearCaches(final TextureStitchEvent.Post e) {
        GfxRenderState.gfxRefresh++;
        CacheType.DEFAULT.call();
    }

    /**
     * Bake all of our custom models.
     */
    public void bakeModels(final ModelBakeEvent event) {
        CacheType.MODEL.call();

        //Chiseled Block
        ChiseledBlockSmartModel smartModel = new ChiseledBlockSmartModel();
        event.getModelRegistry().put(new ModelResourceLocation(ChiselsAndBits2.getInstance().getRegister().CHISELED_BLOCK.get().getRegistryName(), ""), smartModel);
        event.getModelRegistry().put(new ModelResourceLocation(ChiselsAndBits2.getInstance().getRegister().CHISELED_BLOCK.get().getRegistryName(), "inventory"), smartModel);

        //Morphing Bit
        MorphingBitSmartModel morphingModel = new MorphingBitSmartModel();
        event.getModelRegistry().put(new ModelResourceLocation(ChiselsAndBits2.getInstance().getItems().MORPHING_BIT.getRegistryName(), ""), morphingModel);
        event.getModelRegistry().put(new ModelResourceLocation(ChiselsAndBits2.getInstance().getItems().MORPHING_BIT.getRegistryName(), "inventory"), morphingModel);
    }

    /**
     * Call the clean method.
     */
    @SubscribeEvent
    public static void cleanupOnQuit(final ClientPlayerNetworkEvent.LoggedOutEvent e) {
        ChiselsAndBits2.getInstance().getClient().clean();
    }

    /**
     * Handles hotkey presses.
     */
    @SubscribeEvent
    public static void onKeyInput(final InputEvent.KeyInputEvent e) {
        //Return if not in game.
        if (Minecraft.getInstance().player == null) return;

        final ModKeybindings keybindings = ChiselsAndBits2.getInstance().getKeybindings();
        for (ItemModeEnum im : keybindings.modeHotkeys.keySet()) {
            KeyBinding kb = keybindings.modeHotkeys.get(im);
            if (kb.isPressed() && kb.getKeyModifier().isActive(KeyConflictContext.IN_GAME))
                ItemPropertyUtil.setItemMode(Minecraft.getInstance().player, Minecraft.getInstance().player.getHeldItemMainhand(), im);
        }
        for (IMenuAction ma : keybindings.actionHotkeys.keySet()) {
            KeyBinding kb = keybindings.actionHotkeys.get(ma);
            if (kb.isPressed() && kb.getKeyModifier().isActive(KeyConflictContext.IN_GAME)) {
                if(ma.equals(MenuAction.PLACE) || ma.equals(MenuAction.SWAP)) {
                    ItemStack stack = Minecraft.getInstance().player.getHeldItemMainhand();
                    if(stack.getItem() instanceof ChiselMimicItem) {
                        if (((ChiselMimicItem) stack.getItem()).isPlacing(stack))
                            MenuAction.PLACE.trigger();
                        else
                             MenuAction.SWAP.trigger();
                    }
                    continue;
                }
                ma.trigger();
            }
        }
    }


    /**
     * For rendering the ghost selected menu option on the
     * item portrait.
     */
    @SubscribeEvent
    public static void drawLast(final RenderGameOverlayEvent.Post e) {
        if (e.getType() == RenderGameOverlayEvent.ElementType.HOTBAR && ChiselsAndBits2.getInstance().getConfig().enableToolbarIcons.get()) {
            Minecraft.getInstance().getProfiler().startSection("chiselsandbit2-toolbaricons");
            final PlayerEntity player = Minecraft.getInstance().player;
            if (!player.isSpectator()) {
                //If at least one item wants to render something
                if (!hasToolbarIconItem(player.inventory)) return;

                final ItemRenderer ir = Minecraft.getInstance().getItemRenderer();
                GlStateManager.translatef(0, 0, 50);
                GlStateManager.scalef(0.5f, 0.5f, 1);
                GlStateManager.color4f(1, 1, 1, 1.0f);
                Minecraft.getInstance().getTextureManager().bindTexture(AtlasTexture.LOCATION_BLOCKS_TEXTURE);
                RenderHelper.enableGUIStandardItemLighting();
                for (int slot = 8; slot >= -1; --slot) {
                    //-1 is the off-hand
                    ItemStack item = slot == -1 ? player.inventory.offHandInventory.get(0) : player.inventory.mainInventory.get(slot);
                    final int x = (e.getWindow().getScaledWidth() / 2 - 90 + slot * 20 + (slot == -1 ? -9 : 0) + 2) * 2;
                    final int y = (e.getWindow().getScaledHeight() - 16 - 3) * 2;
                    if (item.getItem() instanceof TypedItem && ((TypedItem) item.getItem()).showIconInHotbar()) {
                        final IItemMode mode = ((TypedItem) item.getItem()).getSelectedMode(item);

                        final ResourceLocation sprite = modeIconLocations.get(mode);
                        //Don't render null sprite.
                        if (sprite == null) continue;

                        GlStateManager.translatef(0, 0, 200); //The item models are also rendered 150 higher
                        GlStateManager.enableBlend();
                        int blitOffset = 0;
                        try {
                            Field f = AbstractGui.class.getDeclaredField("blitOffset");
                            f.setAccessible(true);
                            blitOffset = (int) f.get(Minecraft.getInstance().ingameGUI);
                        } catch (Exception rx) {
                            rx.printStackTrace();
                        }
                        AbstractGui.blit(x + 2, y + 2, blitOffset, 16, 16, Minecraft.getInstance().getTextureMap().getSprite(sprite));
                        GlStateManager.disableBlend();
                        GlStateManager.translatef(0, 0, -200);
                    } else if(item.getItem() instanceof StorageItem && ((StorageItem) item.getItem()).showIconInHotbar()) {
                        VoxelWrapper w = ((StorageItem) item.getItem()).getSelected(item);
                        if (w.isEmpty() || w.getType() == VoxelType.COLOURED) continue;
                        ir.renderItemIntoGUI(w.getStack(), x, y);
                    }
                }
                GlStateManager.scalef(2, 2, 1);
                GlStateManager.translatef(0, 0, -50);
                RenderHelper.disableStandardItemLighting();
            }
            Minecraft.getInstance().getProfiler().endSection();
        }
    }

    /**
     * For drawing our custom highlight bounding boxes!
     */
    @SubscribeEvent
    public static void drawHighlights(final DrawBlockHighlightEvent.HighlightBlock e) {
        //Cancel if the draw blocks highlight method successfully rendered a highlight.
        if(ChiselsAndBits2.getInstance().getClient().drawBlockHighlight(e.getPartialTicks()))
            e.setCanceled(true);
    }

    /**
     * For rendering the block placement ghost and static tape measurements.
     */
    @SubscribeEvent
    public static void drawLast(final RenderWorldLastEvent e) {
        if (Minecraft.getInstance().gameSettings.hideGUI) return;

        ClientSide client = ChiselsAndBits2.getInstance().getClient();
        client.renderTapeMeasureBoxes(e.getPartialTicks());
        client.renderPlacementGhost(e.getPartialTicks());
    }

    /**
     * Handles calling the scroll methods on all items implementing IItemScrollWheel.
     */
    @SubscribeEvent
    public static void wheelEvent(final InputEvent.MouseScrollEvent me) {
        final int dwheel = me.getScrollDelta() < 0 ? -1 : me.getScrollDelta() > 0 ? 1 : 0;
        if (me.isCanceled() || dwheel == 0) return;

        final PlayerEntity player = Minecraft.getInstance().player;
        final ItemStack is = player.getHeldItemMainhand();

        if (is.getItem() instanceof IItemScrollWheel && player.isSneaking()) {
            if(((IItemScrollWheel) is.getItem()).scroll(player, is, dwheel))
                me.setCanceled(true);
        }
    }
}
