package dev.hauntclient;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HauntScreen extends Screen {
    private static final int PURPLE = 0xFF9B5CFF;
    private static final int PANEL = 0xF0181022;
    private static final int CARD = 0xFF291B38;
    private static final int MUTED = 0xFFB9A8C9;
    private HauntModules.Category selected = HauntModules.Category.HUD;
    private String search = "";

    public HauntScreen() { super(Text.literal("HAUNT CLIENT V3")); }

    private List<HauntModules.Module> visibleModules() {
        List<HauntModules.Module> list = new ArrayList<>();
        for (HauntModules.Module module : HauntModules.Module.values()) {
            if (module.category != selected) continue;
            String query = search.toLowerCase(Locale.ROOT);
            if (!query.isEmpty()
                && !module.title.toLowerCase(Locale.ROOT).contains(query)
                && !module.description.toLowerCase(Locale.ROOT).contains(query)) continue;
            list.add(module);
        }
        return list;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);
        int w = 640, h = 370, x = (width - w) / 2, y = (height - h) / 2;
        ctx.fill(x - 2, y - 2, x + w + 2, y + h + 2, PURPLE);
        ctx.fill(x, y, x + w, y + h, PANEL);
        ctx.fill(x, y, x + w, y + 48, 0xFF21132E);
        ctx.drawText(textRenderer, "HAUNT CLIENT V3", x + 18, y + 9, 0xFFFFFFFF, true);
        ctx.drawText(textRenderer, "FABRIC 1.21.4  •  MODULAR CLIENT", x + 18, y + 27, MUTED, false);

        int tx = x + 14;
        for (HauntModules.Category category : HauntModules.Category.values()) {
            int tabW = 98;
            ctx.fill(tx, y + 57, tx + tabW - 5, y + 82,
                category == selected ? 0xFF7040A0 : CARD);
            ctx.drawCenteredTextWithShadow(textRenderer, category.name(),
                tx + (tabW - 5) / 2, y + 65,
                category == selected ? 0xFFFFFFFF : MUTED);
            tx += tabW;
        }

        ctx.fill(x + 14, y + 91, x + w - 14, y + 116, 0xFF100B17);
        String searchLabel = search.isEmpty() ? "SEARCH MODULES..." : "SEARCH: " + search + "_";
        ctx.drawText(textRenderer, searchLabel, x + 24, y + 100, search.isEmpty() ? MUTED : 0xFFFFFFFF, false);
        ctx.drawText(textRenderer, "TYPE TO SEARCH  •  BACKSPACE TO EDIT", x + w - 240, y + 100, MUTED, false);

        List<HauntModules.Module> modules = visibleModules();
        if (modules.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, "NO MODULES FOUND",
                x + w / 2, y + 160, MUTED);
        }
        for (int i = 0; i < modules.size(); i++) {
            HauntModules.Module module = modules.get(i);
            int top = y + 126 + i * 39;
            ctx.fill(x + 14, top, x + w - 14, top + 33, CARD);
            ctx.drawText(textRenderer, module.title, x + 25, top + 5, 0xFFFFFFFF, false);
            ctx.drawText(textRenderer, module.description, x + 25, top + 18, MUTED, false);
            if (module == HauntModules.Module.XRAY) {
                ctx.drawText(textRenderer, "BLOCKS: " + HauntModules.getXrayBlockCount()
                    + "  •  RMB: CONFIGURE", x + w - 260, top + 18, 0xFFD7B6FF, false);
            }
            int bx = x + w - 62;
            boolean enabled = HauntModules.isEnabled(module);
            ctx.fill(bx, top + 8, bx + 36, top + 25, enabled ? 0xFF8E4DCE : 0xFF51465A);
            ctx.fill(enabled ? bx + 21 : bx + 3, top + 10,
                enabled ? bx + 33 : bx + 15, top + 23, 0xFFFFFFFF);
        }
        ctx.drawText(textRenderer,
            "LMB: TOGGLE  •  RMB ON X-RAY: BLOCK LIST  •  P / ESC: CLOSE",
            x + 16, y + h - 25, MUTED, false);
        ctx.drawText(textRenderer,
            "Aim Trainer is visual practice only. X-Ray reads client-received blocks.",
            x + 16, y + h - 13, 0xFF8E7BA0, false);
        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int w = 640, h = 370, x = (width - w) / 2, y = (height - h) / 2;
        if (button == 1) {
            List<HauntModules.Module> modules = visibleModules();
            for (int i = 0; i < modules.size(); i++) {
                int top = y + 126 + i * 39;
                if (modules.get(i) == HauntModules.Module.XRAY
                    && mx >= x + 14 && mx <= x + w - 14
                    && my >= top && my <= top + 33) {
                    client.setScreen(new XrayBlockScreen(this));
                    return true;
                }
            }
        }
        if (button == 0) {
            int tx = x + 14;
            for (HauntModules.Category category : HauntModules.Category.values()) {
                if (mx >= tx && mx <= tx + 93 && my >= y + 57 && my <= y + 82) {
                    selected = category;
                    return true;
                }
                tx += 98;
            }
            List<HauntModules.Module> modules = visibleModules();
            for (int i = 0; i < modules.size(); i++) {
                int top = y + 126 + i * 39;
                if (mx >= x + 14 && mx <= x + w - 14
                    && my >= top && my <= top + 33) {
                    HauntModules.toggle(modules.get(i));
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!Character.isISOControl(chr) && search.length() < 24) {
            search += chr;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
            search = search.substring(0, search.length() - 1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public boolean shouldPause() { return false; }
}
