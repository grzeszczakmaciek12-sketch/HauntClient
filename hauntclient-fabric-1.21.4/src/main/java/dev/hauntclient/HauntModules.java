package dev.hauntclient;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.Hand;\nimport net.minecraft.client.option.ParticlesMode;\nimport org.lwjgl.glfw.GLFW;\nimport java.util.ArrayDeque;\nimport java.util.Deque;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

public final class HauntModules {
    public enum Module {
        FULLBRIGHT("Fullbright", "Maximum vanilla brightness", Category.RENDER),\n        FPS_BOOST("FPS Boost", "Reduce particles and entity shadows", Category.RENDER),\n        CUSTOM_CROSSHAIR("Custom Crosshair", "Draw a clean purple custom reticle", Category.HUD),\n        CPS_COUNTER("CPS Counter", "Show recent mouse clicks per second", Category.HUD),
        XRAY("X-Ray Radar", "Radar for ore blocks sent by the server", Category.RENDER),
        COORDINATES("Coordinates", "Show XYZ and dimension", Category.HUD),
        FPS("FPS Counter", "Show current frame rate", Category.HUD),
        KEYSTROKES("Keystrokes", "Show movement key states", Category.HUD),
        ARMOR_HUD("Armor HUD", "Show equipped armor", Category.PLAYER),
        TOGGLE_SPRINT("Toggle Sprint", "Sprint while moving forward", Category.MOVEMENT),
        FLY("Fly", "Flight only when the server grants flight permission", Category.MOVEMENT),
        AIM_TRAINER("Aim Trainer", "Practice reticle only; no automatic aiming", Category.COMBAT),
        TRAINING_AURA("Training Aura", "Automatically hits armor-stand training dummies only", Category.COMBAT),
        BLOCK_INSPECTOR("Block Inspector", "Show the block currently under your crosshair", Category.WORLD),
        FOLLOW_PLAYER("Follow Assist", "Smoothly tracks the nearest player; hold W to follow", Category.PLAYER);

        public final String title, description;
        public final Category category;
        Module(String title, String description, Category category) {
            this.title = title; this.description = description; this.category = category;
        }
    }
    public enum Category { COMBAT, MOVEMENT, RENDER, PLAYER, WORLD, HUD }
    private static final Set<Module> ENABLED = EnumSet.noneOf(Module.class);
    private static double previousGamma = 1.0;
    private static boolean gammaCaptured;\n    private static ParticlesMode previousParticles;\n    private static boolean previousEntityShadows;\n    private static boolean fpsSettingsCaptured;\n    private static boolean previousLeftDown;\n    private static boolean previousRightDown;\n    private static final Deque<Long> CLICK_TIMES = new ArrayDeque<>();
    private static boolean previousAllowFlying;
    private static boolean previousFlying;
    private static int scanIndex;
    private static BlockPos scanOrigin;
    private static PlayerEntity followTarget;
    private static long lastFollowRenderNanos;
    private static final List<String> ORE_RESULTS = new ArrayList<>();
    private static final Set<String> XRAY_BLOCKS = new HashSet<>(List.of("coal_ore", "deepslate_coal_ore", "iron_ore", "deepslate_iron_ore", "gold_ore", "deepslate_gold_ore", "diamond_ore", "deepslate_diamond_ore", "emerald_ore", "deepslate_emerald_ore", "redstone_ore", "deepslate_redstone_ore", "lapis_ore", "deepslate_lapis_ore", "copper_ore", "deepslate_copper_ore", "ancient_debris"));
    private HauntModules() {}

    public static boolean isEnabled(Module module) { return ENABLED.contains(module); }
    public static boolean isXrayBlockSelected(String blockId) { return XRAY_BLOCKS.contains(blockId); }
    public static void toggleXrayBlock(String blockId) {
        if (!XRAY_BLOCKS.remove(blockId)) XRAY_BLOCKS.add(blockId);
        ORE_RESULTS.clear();
        scanIndex = 0;
        scanOrigin = null;
    }
    public static int getXrayBlockCount() { return XRAY_BLOCKS.size(); }

    public static void toggle(Module module) {
        MinecraftClient client = MinecraftClient.getInstance();
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
            if (module == Module.XRAY) { ORE_RESULTS.clear(); scanIndex = 0; scanOrigin = null; }
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
        if (isEnabled(Module.FLY) && client.player != null && !client.player.getAbilities().allowFlying) {
            ENABLED.remove(Module.FLY);
            client.player.sendMessage(net.minecraft.text.Text.literal("§cSerwer odebrał uprawnienie do latania."), true);
        }
        if (isEnabled(Module.CPS_COUNTER) && client.getWindow() != null) {
            long window = client.getWindow().getHandle();
            boolean leftDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_PRESS;
            boolean rightDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_2) == GLFW.GLFW_PRESS;
            long now = System.currentTimeMillis();
            if (leftDown && !previousLeftDown) CLICK_TIMES.addLast(now);
            if (rightDown && !previousRightDown) CLICK_TIMES.addLast(now);
            previousLeftDown = leftDown;
            previousRightDown = rightDown;
            while (!CLICK_TIMES.isEmpty() && now - CLICK_TIMES.peekFirst() > 1000L) CLICK_TIMES.removeFirst();
        }
        if (isEnabled(Module.FOLLOW_PLAYER)) updateFollowAssist(client);
        else followTarget = null;
        if (isEnabled(Module.TRAINING_AURA)) tickTrainingAura(client);
        if (isEnabled(Module.XRAY) && client.player != null && client.world != null) {
            scanNearbyOresIncrementally(client);
        }
    }

    /** Attacks armor-stand dummies only; player entities are never considered. */
    private static void tickTrainingAura(MinecraftClient client) {
        if (client.player == null || client.world == null || client.interactionManager == null) return;
        final double range = 4.5;
        ArmorStandEntity target = null;
        double nearestDistance = range * range;
        for (ArmorStandEntity dummy : client.world.getEntitiesByClass(
            ArmorStandEntity.class,
            client.player.getBoundingBox().expand(range),
            ArmorStandEntity::isAlive
        )) {
            double distance = client.player.squaredDistanceTo(dummy);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                target = dummy;
            }
        }
        if (target != null && client.player.getAttackCooldownProgress(0.5f) >= 1.0f) {
            client.interactionManager.attackEntity(client.player, target);
            client.player.swingHand(Hand.MAIN_HAND);
        }
    }

    private static void updateFollowAssist(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            followTarget = null;
            return;
        }

        PlayerEntity nearest = null;
        double bestDistance = 32.0 * 32.0;
        for (PlayerEntity candidate : client.world.getPlayers()) {
            if (candidate == client.player || candidate.isSpectator() || !candidate.isAlive()) continue;
            double distance = client.player.squaredDistanceTo(candidate);
            if (distance < bestDistance) {
                bestDistance = distance;
                nearest = candidate;
            }
        }
        followTarget = nearest;
        // Camera smoothing is applied by the render-frame callback below.
    }

    /**
     * Interpolates the camera every rendered frame instead of applying a visible
     * step once per game tick. This keeps the on-screen tracking smooth at
     * different frame rates while leaving player movement and attacks untouched.
     */
    public static void renderFollowAssist() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!isEnabled(Module.FOLLOW_PLAYER) || client.player == null
            || client.world == null || followTarget == null || !followTarget.isAlive()) return;

        var from = client.player.getEyePos();
        var to = followTarget.getEyePos();
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float wantedYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float wantedPitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));

        // Exponential interpolation makes the smoothing independent of FPS.
        long now = System.nanoTime();
        float frameSeconds = lastFollowRenderNanos == 0L ? 1.0f / 60.0f
            : Math.min(0.05f, Math.max(0.0f, (now - lastFollowRenderNanos) / 1_000_000_000.0f));
        lastFollowRenderNanos = now;
        float frameFactor = 1.0f - (float) Math.exp(-18.0f * frameSeconds);
        float yaw = client.player.getYaw()
            + MathHelper.wrapDegrees(wantedYaw - client.player.getYaw()) * frameFactor;
        float pitch = client.player.getPitch()
            + (wantedPitch - client.player.getPitch()) * frameFactor;
        client.player.setYaw(yaw);
        client.player.setPitch(MathHelper.clamp(pitch, -90.0f, 90.0f));
    }

    private static void scanNearbyOresIncrementally(MinecraftClient client) {
        // Spread the same local scan over many ticks to avoid a one-tick CPU spike.
        final int widthX = 17;
        final int widthY = 11;
        final int widthZ = 17;
        final int total = widthX * widthY * widthZ;
        final int blocksPerTick = 160;

        if (scanOrigin == null || scanIndex == 0) {
            scanOrigin = client.player.getBlockPos();
            ORE_RESULTS.clear();
        }

        int checked = 0;
        while (checked < blocksPerTick && scanIndex < total && ORE_RESULTS.size() < 6) {
            int dx = scanIndex / (widthY * widthZ) - 8;
            int remainder = scanIndex % (widthY * widthZ);
            int dy = remainder / widthZ - 5;
            int dz = remainder % widthZ - 8;
            BlockPos pos = scanOrigin.add(dx, dy, dz);
            String id = net.minecraft.registry.Registries.BLOCK.getId(
                client.world.getBlockState(pos).getBlock()
            ).getPath();
            if (XRAY_BLOCKS.contains(id)) {
                ORE_RESULTS.add(id.replace('_', ' ') + " "
                    + pos.getX() + " " + pos.getY() + " " + pos.getZ());
            }
            scanIndex++;
            checked++;
        }

        if (scanIndex >= total || ORE_RESULTS.size() >= 6) {
            scanIndex = 0;
            scanOrigin = null;
        }
    }

    public static void registerHud() {
        HudRenderCallback.EVENT.register((context, tickCounter) -> renderHud(context));
    }

    private static void renderHud(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        renderFollowAssist();
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
            drawLine(context, keys, x, y); y += 12;
        }
        if (isEnabled(Module.XRAY)) {
            drawLine(context, "X-RAY RADAR • SERVER-SENT BLOCKS ONLY", x, y); y += 12;
            for (String ore : ORE_RESULTS) { drawLine(context, ore, x, y); y += 12; }
        }
        if (isEnabled(Module.BLOCK_INSPECTOR) && client.crosshairTarget instanceof net.minecraft.util.hit.BlockHitResult hit) {
            var state = client.world.getBlockState(hit.getBlockPos());
            String id = net.minecraft.registry.Registries.BLOCK.getId(state.getBlock()).toString();
            drawLine(context, "BLOCK: " + id, x, y); y += 12;
        }
        if (isEnabled(Module.TRAINING_AURA)) {\n            drawLine(context, "TRAINING AURA: ARMOR STANDS ONLY", x, y); y += 12;\n        }\n        if (isEnabled(Module.FOLLOW_PLAYER)) {
            if (followTarget != null) {
                drawLine(context, "FOLLOW: " + followTarget.getName().getString() + "  " + String.format(java.util.Locale.ROOT, "%.1fm", client.player.distanceTo(followTarget)), x, y);
            } else {
                drawLine(context, "FOLLOW: waiting for a nearby player", x, y);
            }
            y += 12;
        }
        if (isEnabled(Module.CPS_COUNTER)) {
            drawLine(context, "CPS: " + CLICK_TIMES.size(), x, y); y += 12;
        }
        if (isEnabled(Module.CUSTOM_CROSSHAIR)) {
            int cx = context.getScaledWindowWidth() / 2;
            int cy = context.getScaledWindowHeight() / 2;
            int color = 0xFFE7D4FF;
            context.fill(cx - 7, cy, cx - 3, cy + 1, color);
            context.fill(cx + 4, cy, cx + 8, cy + 1, color);
            context.fill(cx, cy - 7, cx + 1, cy - 3, color);
            context.fill(cx, cy + 4, cx + 1, cy + 8, color);
            context.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFFB77A58);
        }
        if (isEnabled(Module.AIM_TRAINER)) {
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
