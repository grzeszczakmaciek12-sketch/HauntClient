package dev.hauntclient;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

public final class HauntModules {
    public enum Module {
        FULLBRIGHT("Fullbright", "Maximum vanilla brightness", Category.RENDER),
        XRAY("X-Ray Radar", "Radar for ore blocks sent by the server", Category.RENDER),
        SERVER_DEMO("Server Demo", "Enable permitted server-side test features", Category.HUD),
        COORDINATES("Coordinates", "Show XYZ and dimension", Category.HUD),
        FPS("FPS Counter", "Show current frame rate", Category.HUD),
        KEYSTROKES("Keystrokes", "Show movement key states", Category.HUD),
        ARMOR_HUD("Armor HUD", "Show equipped armor", Category.PLAYER),
        TOGGLE_SPRINT("Toggle Sprint", "Sprint while moving forward", Category.MOVEMENT),
        FLY("Fly", "Flight only when the server grants flight permission", Category.MOVEMENT),
        AIM_TRAINER("Aim Trainer", "Practice reticle only; no automatic aiming", Category.COMBAT),
        BLOCK_INSPECTOR("Block Inspector", "Show the block currently under your crosshair", Category.WORLD);

        public final String title, description;
        public final Category category;
        Module(String title, String description, Category category) {
            this.title = title; this.description = description; this.category = category;
        }
    }
    public enum Category { COMBAT, MOVEMENT, RENDER, PLAYER, WORLD, HUD }
    private static final Set<Module> ENABLED = EnumSet.noneOf(Module.class);
    private static double previousGamma = 1.0;
    private static boolean gammaCaptured;
    private static boolean previousAllowFlying;
    private static boolean previousFlying;
    private static int scanCooldown;
    private static final List<String> ORE_RESULTS = new ArrayList<>();
    private static final Set<String> XRAY_BLOCKS = new HashSet<>(List.of("coal_ore", "deepslate_coal_ore", "iron_ore", "deepslate_iron_ore", "gold_ore", "deepslate_gold_ore", "diamond_ore", "deepslate_diamond_ore", "emerald_ore", "deepslate_emerald_ore", "redstone_ore", "deepslate_redstone_ore", "lapis_ore", "deepslate_lapis_ore", "copper_ore", "deepslate_copper_ore", "ancient_debris"));
    private HauntModules() {}

    public static boolean isEnabled(Module module) { return ENABLED.contains(module); }
    public static boolean isXrayBlockSelected(String blockId) { return XRAY_BLOCKS.contains(blockId); }
    public static void toggleXrayBlock(String blockId) {
        if (!XRAY_BLOCKS.remove(blockId)) XRAY_BLOCKS.add(blockId);
        ORE_RESULTS.clear();
    }
    public static int getXrayBlockCount() { return XRAY_BLOCKS.size(); }

    public static void toggle(Module module) {
        MinecraftClient client = MinecraftClient.getInstance();
        if ((module == Module.FLY || module == Module.XRAY) && !isEnabled(Module.SERVER_DEMO)) {
            if (client.player != null) client.player.sendMessage(net.minecraft.text.Text.literal("§eNajpierw włącz Server Demo w zakładce HUD."), true);
            return;
        }
        if (module == Module.FLY && client.player != null && !client.player.getAbilities().allowFlying) {
            client.player.sendMessage(net.minecraft.text.Text.literal("§cSerwer nie przyznał uprawnienia do latania."), true);
            return;
        }
        if (ENABLED.remove(module)) {
            if (module == Module.FULLBRIGHT && client.options != null && gammaCaptured) {
                client.options.getGamma().setValue(previousGamma);
                gammaCaptured = false;
            }
            if (module == Module.FLY && client.player != null) {
                var abilities = client.player.getAbilities();
                abilities.flying = previousFlying && abilities.allowFlying;
            }
            if (module == Module.XRAY) ORE_RESULTS.clear();
        } else {
            ENABLED.add(module);
            if (module == Module.FULLBRIGHT && client.options != null) {
                previousGamma = client.options.getGamma().getValue();
                gammaCaptured = true;
                client.options.getGamma().setValue(1.0);
            }
            if (module == Module.FLY && client.player != null) {
                var abilities = client.player.getAbilities();
                previousAllowFlying = abilities.allowFlying;
                previousFlying = abilities.flying;
                // Never grant flight locally: the server must allow it first.
                abilities.flying = true;
            }
        }
    }

    public static void tick(MinecraftClient client) {
        if (!isEnabled(Module.SERVER_DEMO)) {
            if (isEnabled(Module.FLY)) ENABLED.remove(Module.FLY);
            if (isEnabled(Module.XRAY)) ENABLED.remove(Module.XRAY);
            ORE_RESULTS.clear();
            return;
        }
        if (isEnabled(Module.FLY) && client.player != null && !client.player.getAbilities().allowFlying) {
            ENABLED.remove(Module.FLY);
            client.player.sendMessage(net.minecraft.text.Text.literal("§cSerwer odebrał uprawnienie do latania."), true);
        }
        if (isEnabled(Module.XRAY) && client.player != null && client.world != null && scanCooldown-- <= 0) {
            scanCooldown = 20;
            scanNearbyOres(client);
        }
    }

    private static void scanNearbyOres(MinecraftClient client) {
        ORE_RESULTS.clear();
        BlockPos origin = client.player.getBlockPos();
        for (int dx = -8; dx <= 8 && ORE_RESULTS.size() < 6; dx++) {
            for (int dy = -5; dy <= 5 && ORE_RESULTS.size() < 6; dy++) {
                for (int dz = -8; dz <= 8 && ORE_RESULTS.size() < 6; dz++) {
                    BlockPos pos = origin.add(dx, dy, dz);
                    String id = net.minecraft.registry.Registries.BLOCK.getId(client.world.getBlockState(pos).getBlock()).getPath();
                    if (XRAY_BLOCKS.contains(id)) {
                        ORE_RESULTS.add(id.replace('_', ' ') + " " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
                    }
                }
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
        if (isEnabled(Module.SERVER_DEMO)) {
            drawLine(context, "SERVER DEMO: ON", x, y); y += 12;
        }
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
            drawLine(context, keys, x, y); y += 12;
        }
        if (isEnabled(Module.XRAY)) {
            drawLine(context, "X-RAY RADAR • SERVER-SENT BLOCKS ONLY", x, y); y += 12;
            for (String ore : ORE_RESULTS) { drawLine(context, ore, x, y); y += 12; }
        }
        if (isEnabled(Module.BLOCK_INSPECTOR) && client.crosshairTarget instanceof net.minecraft.util.hit.BlockHitResult hit) {\n            var state = client.world.getBlockState(hit.getBlockPos());\n            String id = net.minecraft.registry.Registries.BLOCK.getId(state.getBlock()).toString();\n            drawLine(context, "BLOCK: " + id, x, y); y += 12;\n        }\n        if (isEnabled(Module.AIM_TRAINER)) {
            int cx = context.getScaledWindowWidth() / 2;
            int cy = context.getScaledWindowHeight() / 2;
            context.fill(cx - 10, cy, cx + 11, cy + 1, 0xFFE7D4FF);
            context.fill(cx, cy - 10, cx + 1, cy + 11, 0xFFE7D4FF);
            context.drawText(client.textRenderer, "TRAINING", cx + 14, cy - 4, 0xFFE7D4FF, true);
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
