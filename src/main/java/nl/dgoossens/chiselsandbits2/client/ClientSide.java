package nl.dgoossens.chiselsandbits2.client;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.api.*;
import nl.dgoossens.chiselsandbits2.client.render.RenderingAssistant;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.iterators.ChiselIterator;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.iterators.ChiselTypeIterator;
import nl.dgoossens.chiselsandbits2.common.impl.ChiselModeManager;
import nl.dgoossens.chiselsandbits2.client.gui.RadialMenu;
import nl.dgoossens.chiselsandbits2.client.render.overlay.BlockColorChiseled;
import nl.dgoossens.chiselsandbits2.client.render.overlay.ItemColorBitBag;
import nl.dgoossens.chiselsandbits2.client.render.ter.ChiseledBlockTER;
import nl.dgoossens.chiselsandbits2.common.blocks.ChiseledBlockTileEntity;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel.BitLocation;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel.VoxelBlob;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel.VoxelRegionSrc;
import nl.dgoossens.chiselsandbits2.common.items.ChiselItem;
import nl.dgoossens.chiselsandbits2.common.registry.ModItems;
import nl.dgoossens.chiselsandbits2.common.utils.ChiselUtil;
import nl.dgoossens.chiselsandbits2.common.utils.ModUtil;

import java.lang.reflect.Field;
import java.util.HashMap;

@OnlyIn(Dist.CLIENT)
public class ClientSide {
    //--- GENERAL SETUP ---
    public void setup() {
        ClientRegistry.bindTileEntitySpecialRenderer(ChiseledBlockTileEntity.class, new ChiseledBlockTER());
        Minecraft.getInstance().getBlockColors().register(new BlockColorChiseled(), ChiselsAndBits2.getBlocks().CHISELED_BLOCK);
        final ModItems i = ChiselsAndBits2.getItems();
        Minecraft.getInstance().getItemColors().register(new ItemColorBitBag(),
                i.WHITE_BIT_BAG, i.BLACK_BIT_BAG, i.BLUE_BIT_BAG, i.BROWN_BIT_BAG, i.CYAN_BIT_BAG, i.GRAY_BIT_BAG,
                i.GREEN_BIT_BAG, i.LIGHT_BLUE_BIT_BAG, i.LIGHT_GRAY_BIT_BAG, i.LIME_BIT_BAG, i.MAGENTA_BIT_BAG,
                i.ORANGE_BIT_BAG, i.PINK_BIT_BAG, i.PURPLE_BIT_BAG, i.RED_BIT_BAG, i.YELLOW_BIT_BAG);

        //We've got both normal and mod event bus events.
        MinecraftForge.EVENT_BUS.register(getClass());
        FMLJavaModLoadingContext.get().getModEventBus().register(getClass());
    }

    //--- SPRITES ---
    private static final HashMap<IItemMode, SpriteIconPositioning> chiselModeIcons = new HashMap<>();
    public static TextureAtlasSprite undoIcon;
    public static TextureAtlasSprite redoIcon;
    public static TextureAtlasSprite trashIcon;
    public static TextureAtlasSprite sortIcon;
    public static TextureAtlasSprite swapIcon;
    public static TextureAtlasSprite placeIcon;

    public static TextureAtlasSprite roll_x;
    public static TextureAtlasSprite roll_z;
    public static SpriteIconPositioning getIconForMode(final IItemMode mode) { return chiselModeIcons.get(mode); }

    @SubscribeEvent   //TODO waiting for PR https://github.com/MinecraftForge/MinecraftForge/pull/6032
    public static void registerIconTextures(final TextureStitchEvent.Pre e) {
        /*swapIcon = e.getMap().addSprite( new ResourceLocation(ChiselsAndBits2.MOD_ID, "icons/swap"));
        placeIcon = e.getMap().addSprite( new ResourceLocation(ChiselsAndBits2.MOD_ID, "icons/place"));
        undoIcon = e.getMap().addSprite( new ResourceLocation(ChiselsAndBits2.MOD_ID, "icons/undo"));
        redoIcon = e.getMap().addSprite( new ResourceLocation(ChiselsAndBits2.MOD_ID, "icons/redo"));
        trashIcon = e.getMap().addSprite( new ResourceLocation(ChiselsAndBits2.MOD_ID, "icons/trash"));
        sortIcon = e.getMap().addSprite( new ResourceLocation(ChiselsAndBits2.MOD_ID, "icons/sort"));
        roll_x = e.getMap().addSprite( new ResourceLocation(ChiselsAndBits2.MOD_ID, "icons/roll_x"));
        roll_z = e.getMap().addSprite( new ResourceLocation(ChiselsAndBits2.MOD_ID,"icons/roll_z"));

        for(final ItemMode itemMode : ItemMode.values()) {
            final SpriteIconPositioning sip = new SpriteIconPositioning();

            final ResourceLocation png = new ResourceLocation( ChiselsAndBits2.MOD_ID, "textures/icons/" + itemMode.getTypelessName().toLowerCase() + ".png" );

            sip.sprite = e.getMap().addSprite( new ResourceLocation( ChiselsAndBits2.MOD_ID, "icons/" + itemMode.getTypelessName().toLowerCase() ) );

            try {
                final IResource iresource = Minecraft.getInstance().getResourceManager().getResource(png);
                final BufferedImage bi;
                try {
                    bi = ImageIO.read(iresource.getInputStream());
                } finally {
                    IOUtils.closeQuietly(iresource.getInputStream());
                }

                int bottom = 0;
                int right = 0;
                sip.left = bi.getWidth();
                sip.top = bi.getHeight();

                for ( int x = 0; x < bi.getWidth(); x++ )
                {
                    for ( int y = 0; y < bi.getHeight(); y++ )
                    {
                        final int color = bi.getRGB( x, y );
                        final int a = color >> 24 & 0xff;
                        if ( a > 0 )
                        {
                            sip.left = Math.min( sip.left, x );
                            right = Math.max( right, x );

                            sip.top = Math.min( sip.top, y );
                            bottom = Math.max( bottom, y );
                        }
                    }
                }

                sip.height = bottom - sip.top + 1;
                sip.width = right - sip.left + 1;

                sip.left /= bi.getWidth();
                sip.width /= bi.getWidth();
                sip.top /= bi.getHeight();
                sip.height /= bi.getHeight();
            } catch(final IOException ex) {
                sip.height = 1;
                sip.width = 1;
                sip.left = 0;
                sip.top = 0;
            }
            chiselModeIcons.put(itemMode, sip);
        }*/
    }

    //--- UTILITY METHODS ---
    public PlayerEntity getPlayer() { return Minecraft.getInstance().player; }
    public TextureAtlasSprite getMissingIcon() {
        return Minecraft.getInstance().getTextureMap().getSprite(new ResourceLocation("")); //The missing sprite is returned when an error occurs whilst searching for the texture.
    }
    public void breakSound(final World world, final BlockPos pos, final BlockState state) {
        final Block block = state.getBlock();
        final SoundType soundType = block.getSoundType(state, world, pos, getPlayer());
        world.playSound( pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                soundType.getBreakSound(), SoundCategory.BLOCKS,
                (soundType.getVolume() + 1.0F) / 16.0F,
                soundType.getPitch() * 0.9F, false);
    }

    //--- TICK & RENDERING ---
    private RadialMenu radialMenu = new RadialMenu();
    public RadialMenu getRadialMenu() { return radialMenu; }

    //We currently don't use this because MC doesn't render multiple times per frame anymore. (still investigating)
    /*private static byte frameId = Byte.MIN_VALUE;

    **
     * Return the current frame's id, please note that this id is arbitrary.
     * The id is a byte that is supposed to roll over. (so don't worry)
     *
     * This frame id is stored in TER data to make sure TER's don't render
     * twice or more a frame.
     *
    public static byte getFrameId() { return frameId; }*/

    /**
     * For the logic whether or not the radial menu should be opened etc.
     */
    @SubscribeEvent
    public static void onTick(final TickEvent.ClientTickEvent e) {
        final PlayerEntity player = Minecraft.getInstance().player;
        if(player==null) return; //We're not in-game yet if this happens..
        RadialMenu radialMenu = ChiselsAndBits2.getClient().getRadialMenu();
        if((player.getHeldItemMainhand().getItem() instanceof IItemMenu) || radialMenu.isVisible()) { //Also decrease visibility if you've scrolled of the item.
            //If you've recently clicked (click = force close) but you're not pressing the button anymore we can reset the click state.
            if(radialMenu.hasClicked() && !radialMenu.isPressingButton())
                radialMenu.setClicked(false);

            radialMenu.setPressingButton(ChiselsAndBits2.getKeybindings().modeMenu.isKeyDown());
            if(radialMenu.isPressingButton() && !radialMenu.hasClicked() && (player.getHeldItemMainhand().getItem() instanceof IItemMenu)) {
                //While the key is down, increase the visibility
                radialMenu.setActionUsed(false);
                radialMenu.raiseVisibility();
            } else {
                if(!radialMenu.isActionUsed()) {
                    if(radialMenu.hasSwitchTo() || radialMenu.hasAction()) {
                        final float volume = ChiselsAndBits2.getConfig().radialMenuVolume.get().floatValue();
                        if (volume >= 0.0001f)
                            Minecraft.getInstance().getSoundHandler().play(new SimpleSound(SoundEvents.UI_BUTTON_CLICK, SoundCategory.MASTER, volume, 1.0f, player.getPosition()));
                    }

                    if(radialMenu.hasSwitchTo())
                        ChiselModeManager.changeItemMode(radialMenu.getSwitchTo());

                    if(radialMenu.hasAction()) {
                        switch(radialMenu.getAction()) {
                            case UNDO:
                                System.out.println("UNDO");
                                break;
                            case REDO:
                                System.out.println("REDO"); //TODO add undo/redo buttons
                                break;
                            case ROLL_X:
                                System.out.println("ROLL_X");
                                break;
                            case ROLL_Z:
                                System.out.println("ROLL_Z");
                                break;
                            case PLACE:
                            case REPLACE:
                                ChiselModeManager.changeMenuActionMode(radialMenu.getAction());
                                break;
                            case BLACK:
                            case WHITE:
                            case BLUE:
                            case BROWN:
                            case CYAN:
                            case GRAY:
                            case GREEN:
                            case LIGHT_BLUE:
                            case LIGHT_GRAY:
                            case LIME:
                            case MAGENTA:
                            case ORANGE:
                            case PINK:
                            case PURPLE:
                            case RED:
                            case YELLOW:
                                ChiselModeManager.changeMenuActionMode(radialMenu.getAction());
                                break;
                        }
                    }
                }

                radialMenu.setActionUsed(true);
                radialMenu.decreaseVisibility();
            }
        }
        radialMenu.updateGameFocus();

        //TODO move these to InputEvent
        if(ChiselsAndBits2.getKeybindings().undo.isPressed())
            System.out.println("UNDO");

        if(ChiselsAndBits2.getKeybindings().redo.isPressed())
            System.out.println("REDO");

        //TODO add more keybinds to match C&B1
    }

    /**
     * For rendering the ghost selected menu option on the
     * item portrait.
     * Also for rendering the radial menu when the time comes.
     */
    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void drawLast(final RenderGameOverlayEvent.Post e) {
        if(e.getType() == RenderGameOverlayEvent.ElementType.ALL) {
            Minecraft.getInstance().getProfiler().startSection("chiselsandbit2-radialmenu");
            RadialMenu radialMenu = ChiselsAndBits2.getClient().getRadialMenu();
            if(radialMenu.isVisible()) { //Render if it's visible.
                final MainWindow window = e.getWindow();
                radialMenu.configure(window); //Setup the height/width scales
                if(radialMenu.isVisible()) {
                    if(radialMenu.getMinecraft().isGameFocused())
                        KeyBinding.unPressAllKeys();

                    int i = (int)(radialMenu.getMinecraft().mouseHelper.getMouseX() * (double)window.getScaledWidth() / (double)window.getWidth());
                    int j = (int)(radialMenu.getMinecraft().mouseHelper.getMouseY() * (double)window.getScaledHeight() / (double)window.getHeight());

                    //This comment makes note that the code below is horrible from a forge perspective, but it's great.
                    ForgeHooksClient.drawScreen(radialMenu, i, j, e.getPartialTicks());
                }
            }
            Minecraft.getInstance().getProfiler().endSection();
        }

        if(e.getType() == RenderGameOverlayEvent.ElementType.HOTBAR && ChiselsAndBits2.getConfig().enableToolbarIcons.get()) {
            Minecraft.getInstance().getProfiler().startSection("chiselsandbit2-toolbaricons");
            final PlayerEntity player = Minecraft.getInstance().player;
            if(!player.isSpectator()) {
                final ItemRenderer ir = Minecraft.getInstance().getItemRenderer();
                GlStateManager.translatef(0, 0, 50);
                GlStateManager.scalef(0.5f, 0.5f, 1);
                GlStateManager.color4f(1, 1, 1, 1.0f);
                Minecraft.getInstance().getTextureManager().bindTexture( AtlasTexture.LOCATION_BLOCKS_TEXTURE );
                RenderHelper.enableGUIStandardItemLighting();
                for(int slot = 8; slot >= 0; --slot) {
                    if(player.inventory.mainInventory.get(slot).getItem() instanceof IItemMenu) {
                        final IItemMode mode = ChiselModeManager.getMode(player.inventory.mainInventory.get(slot));
                        final int x = (e.getWindow().getScaledWidth() / 2 - 90 + slot * 20 + 2)*2;
                        final int y = (e.getWindow().getScaledHeight() - 16 - 3)*2;

                        final TextureAtlasSprite sprite = chiselModeIcons.get(mode) == null ? ChiselsAndBits2.getClient().getMissingIcon() : chiselModeIcons.get( mode ).sprite;
                        if(mode instanceof SelectedBlockItemMode) {
                            if(mode.equals(SelectedBlockItemMode.NONE)) continue;
                            ir.renderItemIntoGUI(((SelectedBlockItemMode) mode).getStack(), x, y);
                        } else {
                            GlStateManager.translatef(0, 0, 200); //The item models are also rendered 150 higher
                            GlStateManager.enableBlend();
                            int blitOffset = 0;
                            try {
                                Field f = AbstractGui.class.getDeclaredField("blitOffset");
                                f.setAccessible(true);
                                blitOffset = (int) f.get(Minecraft.getInstance().ingameGUI);
                            } catch(Exception rx) { rx.printStackTrace(); }
                            AbstractGui.blit( x + 2, y + 2, blitOffset, 16, 16, sprite );
                            GlStateManager.disableBlend();
                            GlStateManager.translatef(0, 0, -200);
                        }
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
    @OnlyIn(Dist.CLIENT)
    public static void drawHighlights(final DrawBlockHighlightEvent e) {
        final PlayerEntity player = Minecraft.getInstance().player;
        if(player.getHeldItemMainhand().getItem() instanceof ChiselItem) {
            final RayTraceResult mop = Minecraft.getInstance().objectMouseOver;
            if(mop==null || mop.getType() != RayTraceResult.Type.BLOCK) return;

            final World world = Minecraft.getInstance().world;
            final BitLocation location = new BitLocation((BlockRayTraceResult)  mop, true, BitOperation.REMOVE); //We always show the removal box, never the placement one.
            final TileEntity data = world.getTileEntity(location.blockPos);
            final BlockState state = world.getBlockState(location.blockPos);

            //We only show this box if this block is chiselable and this block at this position is chiselable.
            if(!ChiselUtil.canChiselBlock(state)) return;
            if(!ChiselUtil.canChiselPosition(location.getBlockPos(), player, state, ((BlockRayTraceResult) mop).getFace())) return;

            final IItemMode mode = ChiselModeManager.getMode(player.getHeldItemMainhand());
            final VoxelRegionSrc region = new VoxelRegionSrc(world, location.blockPos, 1);
            final VoxelBlob vb = data instanceof ChiseledBlockTileEntity ? ((ChiseledBlockTileEntity) data).getBlob() : new VoxelBlob();
            if(!(data instanceof ChiseledBlockTileEntity)) vb.fill(ModUtil.getStateId(state));

            final ChiselIterator i = ChiselTypeIterator.create(VoxelBlob.DIMENSION, location.bitX, location.bitY, location.bitZ, region, mode, ((BlockRayTraceResult) mop).getFace(), false);
            final AxisAlignedBB bb = i.getBoundingBox(vb, true);
            RenderingAssistant.drawSelectionBoundingBoxIfExists(bb, location.blockPos, player, e.getPartialTicks(), false);
            e.setCanceled(true);
        }
    }

    /**
     * For rendering the block placement ghost.
     */
    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void drawLast(final RenderWorldLastEvent e) {
        //frameId++; //Increase the frame id every time a new frame is drawn.
        if(Minecraft.getInstance().gameSettings.hideGUI) return;

    }

    //--- ITEM SCROLL ---
    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void wheelEvent(final InputEvent.MouseScrollEvent me) {
        final int dwheel = me.getScrollDelta() < 0 ? -1 : me.getScrollDelta() > 0 ? 1 : 0;
        if(me.isCanceled() || dwheel == 0) return;

        final PlayerEntity player = ChiselsAndBits2.getClient().getPlayer();
        final ItemStack is = player.getHeldItemMainhand();

        if(is.getItem() instanceof IItemScrollWheel && player.isSneaking()) {
            ((IItemScrollWheel) is.getItem()).scroll(player, is, dwheel);
            me.setCanceled(true);
        }
    }
}
