package fi.dy.masa.tweakeroo.event;

import org.apache.commons.lang3.tuple.Pair;
import org.joml.Matrix4f;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.World;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.interfaces.IRenderer;
import fi.dy.masa.malilib.util.ActiveMode;
import fi.dy.masa.malilib.util.InventoryUtils;
import fi.dy.masa.malilib.util.WorldUtils;
import fi.dy.masa.malilib.util.data.Color4f;
import fi.dy.masa.malilib.util.nbt.NbtInventory;
import fi.dy.masa.malilib.util.nbt.NbtKeys;
import fi.dy.masa.tweakeroo.config.Configs;
import fi.dy.masa.tweakeroo.config.FeatureToggle;
import fi.dy.masa.tweakeroo.config.Hotkeys;
import fi.dy.masa.tweakeroo.data.ServerDataSyncer;
import fi.dy.masa.tweakeroo.renderer.InventoryOverlayHandler;
import fi.dy.masa.tweakeroo.renderer.RenderUtils;
import fi.dy.masa.tweakeroo.tweaks.RenderTweaks;

public class RenderHandler implements IRenderer
{
    private static final RenderHandler INSTANCE = new RenderHandler();
    private final MinecraftClient mc;
    private Pair<Entity, NbtCompound> lastEnderItems;

    public RenderHandler()
    {
        this.mc = MinecraftClient.getInstance();
        this.lastEnderItems = null;
    }

    public static RenderHandler getInstance()
    {
        return INSTANCE;
    }

    @Override
    public void onRenderGameOverlayPostAdvanced(DrawContext drawContext, float partialTicks, Profiler profiler, MinecraftClient mc)
    {
        if (FeatureToggle.TWEAK_HOTBAR_SWAP.getBooleanValue() &&
            Hotkeys.HOTBAR_SWAP_BASE.getKeybind().isKeybindHeld())
        {
            RenderUtils.renderHotbarSwapOverlay(mc, drawContext);
        }
        else if (FeatureToggle.TWEAK_HOTBAR_SCROLL.getBooleanValue() &&
                 Hotkeys.HOTBAR_SCROLL.getKeybind().isKeybindHeld())
        {
            RenderUtils.renderHotbarScrollOverlay(mc, drawContext);
        }

        if (FeatureToggle.TWEAK_INVENTORY_PREVIEW.getBooleanValue() &&
            Hotkeys.INVENTORY_PREVIEW.getKeybind().isKeybindHeld())
        {
            /*
            InventoryOverlay.Context context = RayTraceUtils.getTargetInventory(mc);

            if (context != null)
            {
                RenderUtils.renderInventoryOverlay(context, drawContext);
            }
             */

            InventoryOverlayHandler.getInstance().getRenderContext(drawContext, profiler, mc);
        }

        if (FeatureToggle.TWEAK_PLAYER_INVENTORY_PEEK.getBooleanValue() &&
            Hotkeys.PLAYER_INVENTORY_PEEK.getKeybind().isKeybindHeld())
        {
            RenderUtils.renderPlayerInventoryOverlay(mc, drawContext);
        }

        if (FeatureToggle.TWEAK_SNAP_AIM.getBooleanValue() &&
            Configs.Generic.SNAP_AIM_INDICATOR.getBooleanValue())
        {
            RenderUtils.renderSnapAimAngleIndicator(drawContext);
        }

        if (FeatureToggle.TWEAK_ELYTRA_CAMERA.getBooleanValue())
        {
            ActiveMode mode = (ActiveMode) Configs.Generic.ELYTRA_CAMERA_INDICATOR.getOptionListValue();

            if (mode == ActiveMode.ALWAYS || (mode == ActiveMode.WITH_KEY && Hotkeys.ELYTRA_CAMERA.getKeybind().isKeybindHeld()))
            {
                RenderUtils.renderPitchLockIndicator(mc, drawContext);
            }
        }
    }

    @Override
    public void onRenderTooltipLast(DrawContext drawContext, ItemStack stack, int x, int y)
    {
        Item item = stack.getItem();
        if (item instanceof FilledMapItem)
        {
            if (FeatureToggle.TWEAK_MAP_PREVIEW.getBooleanValue() &&
                (Configs.Generic.MAP_PREVIEW_REQUIRE_SHIFT.getBooleanValue() == false || GuiBase.isShiftDown()))
            {
                fi.dy.masa.malilib.render.RenderUtils.renderMapPreview(drawContext, stack, x, y, Configs.Generic.MAP_PREVIEW_SIZE.getIntegerValue(), false);
            }
        }
        else if (stack.getComponents().contains(DataComponentTypes.CONTAINER) && InventoryUtils.shulkerBoxHasItems(stack))
        {
            if (FeatureToggle.TWEAK_SHULKERBOX_DISPLAY.getBooleanValue() &&
                (Configs.Generic.SHULKER_DISPLAY_REQUIRE_SHIFT.getBooleanValue() == false || GuiBase.isShiftDown()))
            {
                fi.dy.masa.malilib.render.RenderUtils.renderShulkerBoxPreview(drawContext, stack, x, y, Configs.Generic.SHULKER_DISPLAY_BACKGROUND_COLOR.getBooleanValue());
            }
        }
        else if (stack.isOf(Items.ENDER_CHEST) && Configs.Generic.SHULKER_DISPLAY_ENDER_CHEST.getBooleanValue())
        {
            if (FeatureToggle.TWEAK_SHULKERBOX_DISPLAY.getBooleanValue() &&
                (Configs.Generic.SHULKER_DISPLAY_REQUIRE_SHIFT.getBooleanValue() == false || GuiBase.isShiftDown()))
            {
                World world = WorldUtils.getBestWorld(this.mc);
                if (world == null || this.mc.player == null)
                {
                    return;
                }
                PlayerEntity player = world.getPlayerByUuid(this.mc.player.getUuid());

                if (player != null)
                {
                    Pair<Entity, NbtCompound> pair = ServerDataSyncer.getInstance().requestEntity(world, player.getId());
                    EnderChestInventory inv;

                    if (pair != null && pair.getRight() != null && pair.getRight().contains(NbtKeys.ENDER_ITEMS))
                    {
                        inv = InventoryUtils.getPlayerEnderItemsFromNbt(pair.getRight(), world.getRegistryManager());
                        this.lastEnderItems = pair;
                    }
                    else if (pair != null && pair.getLeft() instanceof PlayerEntity pe && !pe.getEnderChestInventory().isEmpty())
                    {
                        inv = pe.getEnderChestInventory();
                    }
                    else if (this.lastEnderItems != null)
                    {
                        inv = InventoryUtils.getPlayerEnderItemsFromNbt(this.lastEnderItems.getRight(), world.getRegistryManager());
                    }
                    else
                    {
                        // Last Ditch effort
                        inv = player.getEnderChestInventory();
                    }

                    if (inv != null)
                    {
                        try (NbtInventory nbtInv = NbtInventory.fromInventory(inv))
                        {
                            NbtCompound nbt = new NbtCompound();
                            NbtList list = nbtInv.toNbtList(world.getRegistryManager());

                            nbt.put(NbtKeys.ENDER_ITEMS, list);
                            fi.dy.masa.malilib.render.RenderUtils.renderNbtItemsPreview(drawContext, stack, nbt, x, y, false);
                        }
                        catch (Exception ignored) { }
                    }
                }
            }
        }
        else if (stack.getComponents().contains(DataComponentTypes.BUNDLE_CONTENTS) && InventoryUtils.bundleHasItems(stack))
        {
            if (FeatureToggle.TWEAK_BUNDLE_DISPLAY.getBooleanValue() &&
                (Configs.Generic.BUNDLE_DISPLAY_REQUIRE_SHIFT.getBooleanValue() == false || GuiBase.isShiftDown()))
            {
                fi.dy.masa.malilib.render.RenderUtils.renderBundlePreview(drawContext, stack, x, y, Configs.Generic.BUNDLE_DISPLAY_ROW_WIDTH.getIntegerValue(), Configs.Generic.BUNDLE_DISPLAY_BACKGROUND_COLOR.getBooleanValue());
            }
        }
    }

    @Override
    public void onRenderWorldLastAdvanced(Framebuffer fb, Matrix4f posMatrix, Matrix4f projMatrix, Frustum frustum, Camera camera, BufferBuilderStorage buffers, Profiler profiler)
    {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.player != null)
        {
            RenderTweaks.render(posMatrix, projMatrix, profiler);
            this.renderOverlays(posMatrix, mc);
        }
    }

    private static final float FLEX_OVERLAY_EXPAND = 0.002f;
    private static final float ACCURATE_OUTLINE_EXPAND = 0.001f;
    private static final Color4f FLEXIBLE_OVERLAY_BASE_COLOR = new Color4f(0.7529f, 0.188f, 0.188f, 0.9412f);

    private Color4f applyHue(Color4f base, float hueDegrees)
    {
        float r = 0f, g = 0f, b = 0f, a = 1f;
        try {
            java.lang.reflect.Field fr = base.getClass().getDeclaredField("r");
            java.lang.reflect.Field fg = base.getClass().getDeclaredField("g");
            java.lang.reflect.Field fb = base.getClass().getDeclaredField("b");
            java.lang.reflect.Field fa = base.getClass().getDeclaredField("a");
            fr.setAccessible(true);
            fg.setAccessible(true);
            fb.setAccessible(true);
            fa.setAccessible(true);
            r = fr.getFloat(base);
            g = fg.getFloat(base);
            b = fb.getFloat(base);
            a = fa.getFloat(base);
        } catch (Exception ignored) { }
        int rgbInt = ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | ((int)(b * 255));
        float[] hsv = java.awt.Color.RGBtoHSB((rgbInt >> 16) & 0xFF, (rgbInt >> 8) & 0xFF, rgbInt & 0xFF, null);
        hsv[0] = (hueDegrees % 360f) / 360f;
        int newRgb = java.awt.Color.HSBtoRGB(hsv[0], hsv[1], hsv[2]);
        float newR = ((newRgb >> 16) & 0xFF) / 255f;
        float newG = ((newRgb >> 8) & 0xFF) / 255f;
        float newB = (newRgb & 0xFF) / 255f;
        return new Color4f(newR, newG, newB, a);
    }

    private Color4f blendFlexibleColor(boolean adj, boolean off, boolean rot)
    {
        if (adj == false && off == false && rot == false)
            return null;

        // additive RGB mixing of the primary colours
        float r = 0f, g = 0f, b = 0f;
        if (adj) r += 1f;   // Red
        if (off) g += 1f;   // Green
        if (rot) b += 1f;   // Blue

        float max = Math.max(r, Math.max(g, b));
        if (max > 1f)
        {
            r /= max;
            g /= max;
            b /= max;
        }

        float a = 0.9412f;

        return new Color4f(r, g, b, a);
    }

    private void renderOverlays(Matrix4f posMatrix, MinecraftClient mc)
    {
        Entity entity = mc.getCameraEntity();

        boolean adj = Hotkeys.FLEXIBLE_BLOCK_PLACEMENT_ADJACENT.getKeybind().isKeybindHeld();
        boolean off = Hotkeys.FLEXIBLE_BLOCK_PLACEMENT_OFFSET.getKeybind().isKeybindHeld();
        boolean rot = Hotkeys.FLEXIBLE_BLOCK_PLACEMENT_ROTATION.getKeybind().isKeybindHeld();

        if (FeatureToggle.TWEAK_FLEXIBLE_BLOCK_PLACEMENT.getBooleanValue() &&
            entity != null &&
            mc.crosshairTarget != null &&
            mc.crosshairTarget.getType() == HitResult.Type.BLOCK &&
            (adj || off || rot))
        {
            BlockHitResult hitResult = (BlockHitResult) mc.crosshairTarget;
            Color4f color = blendFlexibleColor(adj, off, rot);
            if (color == null)
            {
                color = FLEXIBLE_OVERLAY_BASE_COLOR;
            }
            fi.dy.masa.malilib.render.RenderUtils.renderBlockTargetingOverlay(
                    entity,
                    hitResult.getBlockPos(),
                    hitResult.getSide(),
                    hitResult.getPos(),
                    color, posMatrix);
        }

        /* Accurate placement outline */
        boolean accurateFeature = FeatureToggle.TWEAK_ACCURATE_BLOCK_PLACEMENT.getBooleanValue();
        boolean accurateIn = Hotkeys.ACCURATE_BLOCK_PLACEMENT_IN.getKeybind().isKeybindHeld();
        boolean accurateReverse = Hotkeys.ACCURATE_BLOCK_PLACEMENT_REVERSE.getKeybind().isKeybindHeld();
        if (accurateFeature && entity != null && mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.BLOCK && (accurateIn || accurateReverse))
        {
            BlockHitResult hitResult = (BlockHitResult) mc.crosshairTarget;
            Color4f color = Configs.Generic.ACCURATE_PLACEMENT_OUTLINE_COLOR.getColor();
            // invert RGB if reverse held
            if (accurateReverse)
            {
                try {
                    java.lang.reflect.Field fr = color.getClass().getDeclaredField("r");
                    java.lang.reflect.Field fg = color.getClass().getDeclaredField("g");
                    java.lang.reflect.Field fb = color.getClass().getDeclaredField("b");
                    java.lang.reflect.Field fa = color.getClass().getDeclaredField("a");
                    fr.setAccessible(true);
                    fg.setAccessible(true);
                    fb.setAccessible(true);
                    fa.setAccessible(true);
                    float rVal = fr.getFloat(color);
                    float gVal = fg.getFloat(color);
                    float bVal = fb.getFloat(color);
                    float aVal = fa.getFloat(color);
                    color = new Color4f(1f - rVal, 1f - gVal, 1f - bVal, aVal);
                } catch (Exception ignored) { }
            }
            float lineWidth = 1f;

            // Render on top of vanilla outline by disabling depth test temporarily
            fi.dy.masa.malilib.render.RenderUtils.renderBlockOutline(hitResult.getBlockPos(), ACCURATE_OUTLINE_EXPAND, lineWidth, color, false);
        }
    }
}
