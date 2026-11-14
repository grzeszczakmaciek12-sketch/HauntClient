package dev.hauntclient;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class XrayBlockScreen extends Screen {
    private static final String[] BLOCKS = {
        "coal_ore", "deepslate_coal_ore",
        "iron_ore", "deepslate_iron_ore",
        "gold_ore", "deepslate_gold_ore",
        "diamond_ore", "deepslate_diamond_ore",
        "emerald_ore", "deepslate_emerald_ore",
        "redstone_ore", "deepslate_redstone_ore",
        "lapis_ore", "deepslate_lapis_ore",
        "copper_ore", "deepslate_copper_ore",
        "ancient_debris", "chest",
        "ender_chest", "spawner",
        "obsidian", "amethyst_block"
    };
    private static final int PURPLE = 0xFF9B5CFF;
    private static final int PANEL = 0xF0181022;
    private static final int CARD = 0xFF291B38;
    private final Screen parent;

    public XrayBlockScreen(Screen parent) {
        super(Text.literal("HAUNT CLIENT • X-RAY BLOCKS"));
        this.parent = parent;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);
        int w = 430, h = 310, x = (width - w) / 2, y = (height - h) / 2;
        ctx.fill(x - 2, y - 2, x + w + 2, y + h + 2, PURPLE);
        ctx.fill(x, y, x + w, y + h, PANEL);
        ctx.fill(x, y, x + w, y + 43, 0xFF21132E);
        ctx.drawText(textRenderer, "X-RAY BLOCK LIST", x + 15, y + 10, 0xFFFFFFFF, true);
        ctx.drawText(textRenderer, "RIGHT-CLICK A BLOCK TO ADD / REMOVE", x + 15, y + 25, 0xFFD7B6FF, false);

        for (int i = 0; i < BLOCKS.length; i++) {
            int col = i % 2;
            int row = i / 2;
            int bx = x + 14 + col * 201;
            int by = y + 53 + row * 21;
            boolean selected = HauntModules.isXrayBlockSelected(BLOCKS[i]);
            ctx.fill(bx, by, bx + 192, by + 18, selected ? 0xFF563477 : CARD);
            String label = BLOCKS[i].replace('_', ' ');
            ctx.drawText(textRenderer, label, bx + 7, by + 5, 0xFFFFFFFF, false);
            ctx.drawText(textRenderer, selected ? "ON" : "OFF", bx + 160, by + 5,
                selected ? 0xFFB9FFB9 : 0xFFB9A8C9, false);
        }
        ctx.drawText(textRenderer, "Selected: " + HauntModules.getXrayBlockCount()
            + "  •  ESC: back  •  results show blocks sent by server",
            x + 14, y + h - 17, 0xFFB9A8C9, false);
        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1) {
            int w = 430, h = 310, x = (width - w) / 2, y = (height - h) / 2;
            for (int i = 0; i < BLOCKS.length; i++) {
                int col = i % 2;
                int row = i / 2;
                int bx = x + 14 + col * 201;
                int by = y + 53 + row * 21;
                if (mouseX >= bx && mouseX <= bx + 192 && mouseY >= by && mouseY <= by + 18) {
                    HauntModules.toggleXrayBlock(BLOCKS[i]);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
