package fi.dy.masa.tweakeroo.mixin.render;

import fi.dy.masa.tweakeroo.config.FeatureToggle;
import fi.dy.masa.tweakeroo.config.Hotkeys;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class MixinWorldRenderer_BlockOutline
{
    @Inject(method = "drawBlockOutline", at = @At("HEAD"), cancellable = true)
    private void tweakeroo$cancelVanillaBlockOutline(CallbackInfo ci)
    {
        if (FeatureToggle.TWEAK_ACCURATE_BLOCK_PLACEMENT.getBooleanValue() &&
            (Hotkeys.ACCURATE_BLOCK_PLACEMENT_IN.getKeybind().isKeybindHeld() ||
             Hotkeys.ACCURATE_BLOCK_PLACEMENT_REVERSE.getKeybind().isKeybindHeld()))
        {
            ci.cancel();
        }
    }
} 