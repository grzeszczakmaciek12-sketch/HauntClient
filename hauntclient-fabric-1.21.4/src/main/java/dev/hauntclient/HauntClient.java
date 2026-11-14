package dev.hauntclient;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class HauntClient implements ClientModInitializer {
    private static KeyBinding openMenu;
    @Override
    public void onInitializeClient() {
        openMenu=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.hauntclient.open",
            InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_X,"category.hauntclient"));
        HauntModules.registerHud();
        // Keep all vanilla title-screen buttons; add a shortcut to the Haunt panel.
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof TitleScreen) {
                screen.addDrawableChild(ButtonWidget.builder(Text.literal("HAUNT CLIENT"),
                    button -> client.setScreen(new HauntScreen()))
                    .dimensions(scaledWidth - 116, 8, 108, 22).build());
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while(openMenu.wasPressed()) if(client.currentScreen==null) client.setScreen(new HauntScreen());
            HauntModules.tick(client);
            if(HauntModules.isEnabled(HauntModules.Module.TOGGLE_SPRINT)&&client.player!=null
                &&client.options.forwardKey.isPressed()&&!client.player.isSneaking()) client.player.setSprinting(true);
        });
    }
}
