package dev.hauntclient;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HauntScreen extends Screen {
    private static final int ORANGE = 0xFFFF8A24;
    private static final int GOLD = 0xFFFFC15A;
    private static final int PANEL = 0xF0111020;
    private static final int CARD = 0xFF211B31;
    private static final int CARD_HOVER = 0xFF30233F;
    private static final int MUTED = 0xFFB7A9C8;
    private static final int WHITE = 0xFFFFF4E5;
    private static final int PURPLE = 0xFF9A69C8;

    private HauntModules.Category selected = HauntModules.Category.HUD;
    private String search = "";
    private double scrollOffset = 0;
    private HauntModules.Module bindingModule = null;

    public HauntScreen() { super(Text.literal("HAUNT CLIENT V3")); }

    private List<HauntModules.Module> visibleModules() {
        List<HauntModules.Module> list = new ArrayList<>();
        String query = search.toLowerCase(Locale.ROOT);
        for (HauntModules.Module module : HauntModules.Module.values()) {
            if (module.category != selected) continue;
            if (!query.isEmpty()
                && !module.title.toLowerCase(Locale.ROOT).contains(query)
                && !module.description.toLowerCase(Locale.ROOT).contains(query)) continue;
            list.add(module);
        }
        return list;
    }

    private void drawPumpkin(DrawContext ctx, int x, int y, int scale) {
        int s = scale;
        // Pixel-art pumpkin: warm body, ribs, stem and a tiny carved face.
        ctx.fill(x + 2*s, y, x + 5*s, y + s, 0xFF688D3B);
        ctx.fill(x + s, y + s, x + 6*s, y + 2*s, GOLD);
        ctx.fill(x, y + 2*s, x + 7*s, y + 5*s, ORANGE);
        ctx.fill(x + s, y + 5*s, x + 6*s, y + 6*s, 0xFFE66C1E);
        ctx.fill(x + 2*s, y + 2*s, x + 3*s, y + 5*s, 0xFFFFB044);
        ctx.fill(x + 4*s, y + 2*s, x + 5*s, y + 5*s, 0xFFFFB044);
        ctx.fill(x + 2*s, y + 3*s, x + 3*s, y + 4*s, 0xFF382039);
        ctx.fill(x + 4*s, y + 3*s, x + 5*s, y + 4*s, 0xFF382039);
        ctx.fill(x + 3*s, y + 4*s, x + 4*s, y + 5*s, 0xFF382039);
    }

    private void drawStar(DrawContext ctx, int x, int y, int color) {
        ctx.fill(x, y, x + 2, y + 2, color);
        ctx.fill(x - 2, y + 1, x + 4, y + 2, color);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);
        int w = Math.min(700, width - 24), h = Math.min(410, height - 24);
        int x = (width - w) / 2, y = (height - h) / 2;
        int side = 158;
        ctx.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xFF6C3A78);
        ctx.fill(x, y, x + w, y + h, PANEL);
        ctx.fill(x, y, x + w, y + 66, 0xFF1D142B);
        ctx.fill(x, y + 64, x + w, y + 67, 0xFFB65D37);
        ctx.fill(x, y + 67, x + side, y + h, 0xFF171323);
        drawPumpkin(ctx, x + 18, y + 14, 5);
        ctx.drawText(textRenderer, "HAUNT CLIENT", x + 76, y + 15, WHITE, true);
        ctx.drawText(textRenderer, "V3  /  HALLOWEEN EDITION", x + 77, y + 32, GOLD, false);
        ctx.drawText(textRenderer, "THE NIGHT IS YOURS", x + 77, y + 47, MUTED, false);
        drawStar(ctx, x + w - 105, y + 19, GOLD);
        drawStar(ctx, x + w - 83, y + 39, PURPLE);
        drawPumpkin(ctx, x + w - 60, y + 13, 5);

        ctx.drawText(textRenderer, "NIGHT MENU", x + 17, y + 83, GOLD, true);
        int catY = y + 103;
        for (HauntModules.Category category : HauntModules.Category.values()) {
            boolean active = category == selected;
            int top = catY;
            boolean hover = mouseX >= x + 10 && mouseX <= x + side - 10
                && mouseY >= top && mouseY <= top + 31;
            ctx.fill(x + 10, top, x + side - 10, top + 31,
                active ? 0xFF57334F : (hover ? CARD_HOVER : 0xFF211B30));
            if (active) ctx.fill(x + 10, top + 5, x + 13, top + 26, ORANGE);
            ctx.drawText(textRenderer, category.name(), x + 22, top + 10,
                active ? WHITE : MUTED, active);
            catY += 37;
        }

        int mainX = x + side + 18;
        int mainW = w - side - 36;
        ctx.drawText(textRenderer, selected.name() + " MODULES", mainX, y + 83, WHITE, true);
        ctx.drawText(textRenderer, "Choose what follows you through the fog.", mainX, y + 98, MUTED, false);
        ctx.fill(mainX, y + 119, mainX + mainW, y + 146, 0xFF100D19);
        ctx.fill(mainX, y + 119, mainX + 2, y + 146, ORANGE);
        ctx.drawText(textRenderer, search.isEmpty() ? "⌕  SEARCH THE DARK..." : "⌕  " + search + "_",
            mainX + 11, y + 128, search.isEmpty() ? MUTED : WHITE, false);
        ctx.drawText(textRenderer, "TYPE TO FILTER", mainX + mainW - 100, y + 128, 0xFF766985, false);

        List<HauntModules.Module> modules = visibleModules();
        int cardW = (mainW - 10) / 2;
        int listTop = y + 157;
        int listBottom = y + h - 35;
        int listHeight = Math.max(0, listBottom - listTop);
        int rows = (modules.size() + 1) / 2;
        int contentHeight = Math.max(0, rows * 67 - 9);
        double maxScroll = Math.max(0, contentHeight - listHeight);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        ctx.enableScissor(mainX, listTop, mainX + mainW, listBottom);
        for (int i = 0; i < modules.size(); i++) {
            HauntModules.Module module = modules.get(i);
            int col = i % 2, row = i / 2;
            int left = mainX + col * (cardW + 10);
            int top = (int)(y + 157 + row * 67 - scrollOffset);
            int right = left + cardW;
            boolean hover = mouseX >= left && mouseX <= right && mouseY >= top && mouseY <= top + 58;
            boolean enabled = HauntModules.isEnabled(module);
            ctx.fill(left, top, right, top + 58, hover ? CARD_HOVER : CARD);
            ctx.fill(left, top, left + 3, top + 58, enabled ? ORANGE : 0xFF55465F);
            ctx.drawText(textRenderer, module.title, left + 12, top + 9, WHITE, true);
            String desc = module.description;
            int maxDesc = cardW - 75;
            while (textRenderer.getWidth(desc) > maxDesc && desc.length() > 4)
                desc = desc.substring(0, desc.length() - 4) + "...";
            ctx.drawText(textRenderer, desc, left + 12, top + 27, MUTED, false);
            int bx = right - 48;
            ctx.fill(bx, top + 19, bx + 34, top + 37, enabled ? 0xFFB65D37 : 0xFF51465A);
            ctx.fill(enabled ? bx + 19 : bx + 3, top + 21,
                enabled ? bx + 31 : bx + 15, top + 35, WHITE);
            ctx.drawText(textRenderer, (bindingModule == module ? "PRESS KEY..." : "BIND: " + HauntClient.getModuleBindName(module)),
                left + 12, top + 43, bindingModule == module ? GOLD : 0xFF9D8BAD, false);
        }

        if (modules.isEmpty())
            ctx.drawCenteredTextWithShadow(textRenderer, "NO SPIRITS FOUND",
                mainX + mainW / 2, y + 190, MUTED);
        ctx.disableScissor();

        ctx.fill(x + 1, y + h - 29, x + w - 1, y + h - 1, 0xFF171323);
        drawPumpkin(ctx, x + 14, y + h - 23, 2);
        ctx.drawText(textRenderer, "LMB: TOGGLE  •  MMB: SET BIND  •  P / ESC: CLOSE",
            x + 37, y + h - 19, MUTED, false);
        ctx.drawText(textRenderer, "A LITTLE MAGIC. A LOT OF ATMOSPHERE.",
            x + w - 204, y + h - 19, 0xFFB77A58, false);
        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int w = Math.min(700, width - 24), h = Math.min(410, height - 24);
        int x = (width - w) / 2, y = (height - h) / 2;
        int side = 158;
        if (button == 0) {
            int catY = y + 103;
            for (HauntModules.Category category : HauntModules.Category.values()) {
                if (mx >= x + 10 && mx <= x + side - 10
                    && my >= catY && my <= catY + 31) {
                    selected = category;
                    scrollOffset = 0;
                    return true;
                }
                catY += 37;
            }
        }
        int mainX = x + side + 18;
        int mainW = w - side - 36;
        List<HauntModules.Module> modules = visibleModules();
        int cardW = (mainW - 10) / 2;
        int listTop = y + 157;
        int listBottom = y + h - 35;
        int rows = (modules.size() + 1) / 2;
        int contentHeight = Math.max(0, rows * 67 - 9);
        int listHeight = Math.max(0, listBottom - listTop);
        double maxScroll = Math.max(0, contentHeight - listHeight);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        for (int i = 0; i < modules.size(); i++) {
            int col = i % 2, row = i / 2;
            int left = mainX + col * (cardW + 10);
            int top = (int)(y + 157 + row * 67 - scrollOffset);
            if (my >= listTop && my <= listBottom
                && mx >= left && mx <= left + cardW && my >= top && my <= top + 58) {
                if (button == 2) { bindingModule = modules.get(i); return true; }
                if (button == 1 && modules.get(i) == HauntModules.Module.XRAY) {
                    client.setScreen(new XrayBlockScreen(this));
                    return true;
                }
                if (button == 1 && modules.get(i) == HauntModules.Module.ITEM_ESP) {
                    HauntModules.cycleItemEspColor();
                    return true;
                }
                if (button == 1 && modules.get(i) == HauntModules.Module.TRACERS) {
                    HauntModules.cycleTracerTarget();
                    return true;
                }
                if (button == 0) {
                    HauntModules.toggle(modules.get(i));
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int w = Math.min(700, width - 24), h = Math.min(410, height - 24);
        int x = (width - w) / 2, y = (height - h) / 2;
        int mainX = x + 158 + 18;
        int mainW = w - 158 - 36;
        int listTop = y + 157;
        int listBottom = y + h - 35;
        if (mouseX >= mainX && mouseX <= mainX + mainW && mouseY >= listTop && mouseY <= listBottom) {
            int rows = (visibleModules().size() + 1) / 2;
            int contentHeight = Math.max(0, rows * 67 - 9);
            int listHeight = Math.max(0, listBottom - listTop);
            double maxScroll = Math.max(0, contentHeight - listHeight);
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - verticalAmount * 42));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
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
