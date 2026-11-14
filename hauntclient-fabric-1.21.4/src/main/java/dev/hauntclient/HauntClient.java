package dev.hauntclient;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class HauntClient implements ClientModInitializer {
    private static KeyBinding openMenu;

    @Override
    public void onInitializeClient() {
        openMenu = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.hauntclient.open",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_P,
            "category.hauntclient"
        ));

        HauntModules.registerHud();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenu.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new HauntScreen());
                }
            }

            HauntModules.tick(client);

            if (HauntModules.isEnabled(HauntModules.Module.TOGGLE_SPRINT)
                && client.player != null
                && client.options.forwardKey.isPressed()
                && !client.player.isSneaking()) {
                client.player.setSprinting(true);
            }
        });
    }
}
