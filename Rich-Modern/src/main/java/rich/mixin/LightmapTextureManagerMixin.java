package rich.mixin;

import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rich.Initialization;
import rich.modules.impl.render.FullBright;
import rich.modules.impl.render.NoRender;

@Mixin(LightmapTextureManager.class)
public class LightmapTextureManagerMixin {

    @Inject(method = "update", at = @At("RETURN"))
    private void onUpdate(CallbackInfo ci) {
        // FullBright - просто заглушка, чтобы не было ошибок
        if (Initialization.getInstance().getManager().getModuleProvider().get(FullBright.class).isState()) {
            // TODO: реализовать FullBright для 1.21.11
        }
    }

    @Inject(method = "getDarkness", at = @At("HEAD"), cancellable = true)
    private void removeDarknessEffect(CallbackInfoReturnable<Float> cir) {
        try {
            NoRender noRender = NoRender.getInstance();
            if (noRender != null && noRender.isState() && noRender.modeSetting.isSelected("Darkness")) {
                cir.setReturnValue(0.0F);
            }
        } catch (Exception ignored) {
            // Игнорируем ошибки, чтобы не крашить игру
        }
    }
}