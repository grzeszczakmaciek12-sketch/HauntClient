package dev.hauntclient.mixin;

import dev.hauntclient.Freelook;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public abstract class MouseMixin {
    @Inject(method = "updateMouse", at = @At("HEAD"), cancellable = true)
    private void hauntclient$handleFreelook(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (Freelook.handleMouse(client)) ci.cancel();
    }
}
