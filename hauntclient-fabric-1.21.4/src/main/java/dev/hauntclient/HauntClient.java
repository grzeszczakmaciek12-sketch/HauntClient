package dev.hauntclient;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import java.util.EnumMap;
import java.util.Map;

public class HauntClient implements ClientModInitializer {
    private static KeyBinding openMenu;
    private static final Map<HauntModules.Module, KeyBinding> MODULE_BINDS = new EnumMap<>(HauntModules.Module.class);

    public static void setModuleBind(HauntModules.Module module, int keyCode, int scanCode) {
        KeyBinding binding = MODULE_BINDS.get(module);
        if (binding != null) {
            binding.setBoundKey(InputUtil.fromKeyCode(keyCode, scanCode));
            // Rebuild Minecraft's key-to-binding lookup after changing a bind at runtime.
            KeyBinding.updateKeysByCode();
            MinecraftClientAccess.saveOptions();
        }
    }

    public static String getModuleBindName(HauntModules.Module module) {
        KeyBinding binding = MODULE_BINDS.get(module);
        return binding == null ? "NONE" : binding.getBoundKeyLocalizedText().getString();
    }

    private static final class MinecraftClientAccess {
        private static void saveOptions() {
            net.minecraft.client.MinecraftClient.getInstance().options.write();
        }
    }

    @Override
    public void onInitializeClient() {
        openMenu = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.hauntclient.open",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_P,
            "category.hauntclient"
        ));

        for (HauntModules.Module module : HauntModules.Module.values()) {
            KeyBinding binding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.hauntclient.module." + module.name().toLowerCase(java.util.Locale.ROOT),
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                "category.hauntclient"
            ));
            MODULE_BINDS.put(module, binding);
        }

        HauntModules.registerHud();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenu.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new HauntScreen());
                }
            }

            if (client.currentScreen == null) {
                for (Map.Entry<HauntModules.Module, KeyBinding> entry : MODULE_BINDS.entrySet()) {
                    while (entry.getValue().wasPressed()) HauntModules.toggle(entry.getKey());
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
