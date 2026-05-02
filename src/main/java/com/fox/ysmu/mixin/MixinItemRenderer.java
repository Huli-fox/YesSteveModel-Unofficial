package com.fox.ysmu.mixin;

import com.fox.ysmu.client.compat.AngelicaCompat;
import com.fox.ysmu.client.renderer.FirstPersonHandRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ItemRenderer.class, priority = 900)
public abstract class MixinItemRenderer {

    @Inject(method = "renderItemInFirstPerson", at = @At("HEAD"), cancellable = true)
    private void ysmu$renderAngelicaCustomHand(float partialTicks, CallbackInfo ci) {
        if (!AngelicaCompat.isRenderingSolidHandPass()) {
            return;
        }

        ItemRenderer itemRenderer = (ItemRenderer) (Object) this;
        if (FirstPersonHandRenderer.tryRenderInActiveFirstPersonPass(
            Minecraft.getMinecraft(),
            itemRenderer,
            partialTicks,
            AngelicaCompat.shouldRenderOffhandInCurrentHandPass())) {
            AngelicaCompat.resetFirstPersonItemId();
            ci.cancel();
        }
    }
}
