package dev.hauntclient;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

import java.util.EnumSet;
import java.util.Set;

public final class HauntModules {
    public enum Module {
        FULLBRIGHT("Fullbright", "Maximum vanilla brightness", Category.RENDER),
        COORDINATES("Coordinates", "Show XYZ and dimension", Category.HUD),
        FPS("FPS Counter", "Show current frame rate", Category.HUD),
        KEYSTROKES("Keystrokes", "Show movement key states", Category.HUD),
        ARMOR_HUD("Armor HUD", "Show equipped armor", Category.PLAYER),
        TOGGLE_SPRINT("Toggle Sprint", "Sprint while moving forward", Category.MOVEMENT);

        public final String title, description;
        public final Category category;
        Module(String title, String description, Category category) {
            this.title = title; this.description = description; this.category = category;
        }
    }
    public enum Category { RENDER, MOVEMENT, PLAYER, HUD }
    private static final Set<Module> ENABLED = EnumSet.noneOf(Module.class);
    private static double previousGamma = 1.0;
    private static boolean gammaCaptured;
    private HauntModules() {}

    public static boolean isEnabled(Module module) { return ENABLED.contains(module); }

    public static void toggle(Module module) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (ENABLED.remove(module)) {
            if (module == Module.FULLBRIGHT && client.options != null && gammaCaptured) {
                client.options.getGamma().setValue(previousGamma);
                gammaCaptured = false;
            }
        } else {
            ENABLED.add(module);
            if (module == Module.FULLBRIGHT && client.options != null) {
                previousGamma = client.options.getGamma().getValue();
                gammaCaptured = true;
                client.options.getGamma().setValue(1.0);
            }
        }
    }

    public static void registerHud() {
        HudRenderCallback.EVENT.register((context, tickCounter) -> renderHud(context));
    }

    private static void renderHud(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.options.hudHidden) return;
        int x = 8, y = 8;
        if (isEnabled(Module.FPS)) {
            drawLine(context, "FPS: " + client.getCurrentFps(), x, y); y += 12;
        }
        if (isEnabled(Module.COORDINATES)) {
            var pos = client.player.getBlockPos();
            String dim = client.world.getRegistryKey().getValue().getPath();
            drawLine(context, "XYZ: " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " [" + dim + "]", x, y);
            y += 12;
        }
        if (isEnabled(Module.KEYSTROKES)) {
            String keys = (client.options.forwardKey.isPressed() ? "[W]" : " W ")
                + (client.options.leftKey.isPressed() ? "[A]" : " A ")
                + (client.options.backKey.isPressed() ? "[S]" : " S ")
                + (client.options.rightKey.isPressed() ? "[D]" : " D ")
                + (client.options.jumpKey.isPressed() ? " [SPACE]" : "  SPACE ");
            drawLine(context, keys, x, y);
        }
        if (isEnabled(Module.ARMOR_HUD)) {
            int ax = 8, ay = context.getScaledWindowHeight() - 22;
            for (int i = 3; i >= 0; i--) {
                ItemStack stack = client.player.getInventory().getArmorStack(i);
                if (!stack.isEmpty()) { context.drawItem(stack, ax, ay); ax += 20; }
            }
        }
    }

    private static void drawLine(DrawContext context, String text, int x, int y) {
        int width = MinecraftClient.getInstance().textRenderer.getWidth(text);
        context.fill(x - 3, y - 2, x + width + 4, y + 11, 0x990D0715);
        context.drawText(MinecraftClient.getInstance().textRenderer, text, x, y, 0xFFE7D4FF, true);
    }
}
