package dev.hauntclient.mixin;

import dev.hauntclient.HauntModules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.screen.PlayerScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class XCarryMixin {
    @Inject(method = "closeHandledScreen", at = @At("HEAD"), cancellable = true)
    private void hauntclient$keepCraftingItems(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!HauntModules.isEnabled(HauntModules.Module.XCARRY)
            || client.player != (Object) this
            || client.player == null
            || client.player.currentScreenHandler != client.player.playerScreenHandler) {
            return;
        }

        PlayerScreenHandler handler = client.player.playerScreenHandler;
        // Slots 1-4 are the 2x2 crafting input grid in the player inventory screen.
        for (int slot = 1; slot <= 4; slot++) {
            if (handler.getSlot(slot).hasStack()) {
                ci.cancel();
                return;
            }
        }
    }
}
