package dev.hauntclient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;

import java.nio.DoubleBuffer;

/** Detached view rotation while keeping the local player's body rotation unchanged. */
public final class Freelook {
    private static boolean active;
    private static float cameraYaw;
    private static float cameraPitch;
    private static float playerYaw;
    private static float playerPitch;
    private static double lastMouseX;
    private static double lastMouseY;
    private static boolean mousePositionInitialized;

    private Freelook() {}

    public static boolean isActive() {
        return active;
    }

    public static void start(MinecraftClient client) {
        if (client.player == null || client.getWindow() == null) return;
        active = true;
        playerYaw = client.player.getYaw();
        playerPitch = client.player.getPitch();
        cameraYaw = playerYaw;
        cameraPitch = playerPitch;
        updateMousePosition(client);
    }

    public static void stop(MinecraftClient client) {
        active = false;
        mousePositionInitialized = false;
        if (client.player != null) {
            client.player.setYaw(playerYaw);
            client.player.setPitch(playerPitch);
            client.player.setHeadYaw(playerYaw);
            client.player.setBodyYaw(playerYaw);
        }
    }

    public static void tick(MinecraftClient client) {
        if (!active) return;
        if (client.player == null || client.world == null) {
            stop(client);
            return;
        }
        client.player.setYaw(playerYaw);
        client.player.setPitch(playerPitch);
        client.player.setHeadYaw(playerYaw);
        client.player.setBodyYaw(playerYaw);
    }

    /** Called from Mouse.updateMouse; suppresses vanilla player rotation while active. */
    public static boolean handleMouse(MinecraftClient client) {
        if (!active || client.player == null || client.getWindow() == null) return false;
        double x;
        double y;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            DoubleBuffer xb = stack.mallocDouble(1);
            DoubleBuffer yb = stack.mallocDouble(1);
            GLFW.glfwGetCursorPos(client.getWindow().getHandle(), xb, yb);
            x = xb.get(0);
            y = yb.get(0);
        }

        if (!mousePositionInitialized) {
            lastMouseX = x;
            lastMouseY = y;
            mousePositionInitialized = true;
            return true;
        }

        double dx = x - lastMouseX;
        double dy = y - lastMouseY;
        lastMouseX = x;
        lastMouseY = y;

        double sensitivity = client.options.getMouseSensitivity().getValue();
        double scaled = sensitivity * 0.6 + 0.2;
        double factor = scaled * scaled * scaled * 8.0 * 0.15;
        cameraYaw += (float)(dx * factor);
        cameraPitch = MathHelper.clamp(cameraPitch - (float)(dy * factor), -90.0f, 90.0f);
        return true;
    }

    private static void updateMousePosition(MinecraftClient client) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            DoubleBuffer xb = stack.mallocDouble(1);
            DoubleBuffer yb = stack.mallocDouble(1);
            GLFW.glfwGetCursorPos(client.getWindow().getHandle(), xb, yb);
            lastMouseX = xb.get(0);
            lastMouseY = yb.get(0);
            mousePositionInitialized = true;
        }
    }

    public static float getCameraYaw() {
        return cameraYaw;
    }

    public static float getCameraPitch() {
        return cameraPitch;
    }
}
