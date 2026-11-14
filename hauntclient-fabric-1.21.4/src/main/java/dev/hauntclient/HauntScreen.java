package dev.hauntclient;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class HauntScreen extends Screen {
    private static final int PURPLE = 0xFF9B5CFF;
    private static final int PANEL = 0xEE171020;
    private static final int CARD = 0xFF241632;
    private static final int MUTED = 0xFFB9A8C9;
    private final List<ModuleToggle> modules = new ArrayList<>();

    public HauntScreen() {
        super(Text.literal("HAUNT CLIENT"));
        modules.add(new ModuleToggle("Auto Fish", "Fishing practice toggle"));
        modules.add(new ModuleToggle("Auto Drop", "Inventory test toggle"));
        modules.add(new ModuleToggle("Auto Trap", "Practice-world toggle"));
        modules.add(new ModuleToggle("Aim Trainer", "Local target practice"));
        modules.add(new ModuleToggle("Combat Dummy", "Training-only overlay"));
    }

    @Override
    protected void init() {
        // Buttons are drawn and handled directly to keep the menu compact.
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);
        int w = 330, h = 260;
        int x = (width - w) / 2, y = (height - h) / 2;

        ctx.fill(x - 2, y - 2, x + w + 2, y + h + 2, PURPLE);
        ctx.fill(x, y, x + w, y + h, PANEL);
        ctx.fill(x, y, x + w, y + 42, 0xFF20112D);
        ctx.drawText(textRenderer, "☾ HAUNT CLIENT", x + 14, y + 10, 0xFFFFFFFF, false);
        ctx.drawText(textRenderer, "FABRIC 1.21.4  •  HALLOWEEN BUILD", x + 14, y + 26, MUTED, false);

        int rowY = y + 54;
        for (int i = 0; i < modules.size(); i++) {
            ModuleToggle module = modules.get(i);
            int top = rowY + i * 37;
            ctx.fill(x + 12, top, x + w - 12, top + 30, CARD);
            ctx.drawText(textRenderer, module.name, x + 22, top + 5, 0xFFFFFFFF, false);
            ctx.drawText(textRenderer, module.description, x + 22, top + 17, MUTED, false);
            int bx = x + w - 58;
            int color = module.enabled ? 0xFF8E4DCE : 0xFF51465A;
            ctx.fill(bx, top + 7, bx + 34, top + 23, color);
            ctx.fill(module.enabled ? bx + 19 : bx + 3, top + 9,
                     module.enabled ? bx + 31 : bx + 15, top + 21, 0xFFFFFFFF);
        }

        ctx.drawText(textRenderer, "Click a row to toggle • ESC to close", x + 14, y + h - 18, MUTED, false);
        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int w = 330, h = 260;
            int x = (width - w) / 2, y = (height - h) / 2;
            int rowY = y + 54;
            for (int i = 0; i < modules.size(); i++) {
                int top = rowY + i * 37;
                if (mouseX >= x + 12 && mouseX <= x + w - 12 &&
                    mouseY >= top && mouseY <= top + 30) {
                    modules.get(i).enabled = !modules.get(i).enabled;
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldPause() { return false; }

    private static class ModuleToggle {
        final String name;
        final String description;
        boolean enabled;
        ModuleToggle(String name, String description) {
            this.name = name;
            this.description = description;
        }
    }
}
