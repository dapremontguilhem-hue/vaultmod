package vaultodds;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import vaultodds.VaultOdds.Target;

/** Écran de réglages : choix de l'objet cible, panneau, format des probabilités. */
public class TargetScreen extends Screen {
    private static final int TEXT = 0xFFE0E0E0;
    private static final int DIM = 0xFF909090;
    private static final int GOLD = 0xFFFFD34D;
    private static final int WARN = 0xFFFF6B5A;
    private static final int WARN_SUB = 0xFFD8C8A8;
    private static final int CELL_H = 16;

    private final Screen parent;
    private boolean expanded = false;
    private int scroll = 0;
    private int panelX, panelW, dropY, listY, listH, cols, cellW;

    public TargetScreen(Screen parent) {
        super(Component.literal("Vault Odds"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelW = Math.min(this.width - 40, 300);
        panelX = (this.width - panelW) / 2;
        dropY = 128;
        listY = dropY + 20;
        cols = panelW >= 260 ? 2 : 1;
        cellW = panelW / cols;
        int rows = (VaultOdds.TARGETS.size() + cols - 1) / cols;
        listH = Math.max(CELL_H, Math.min(rows * CELL_H, this.height - listY - 34));

        int gap = 4;
        int bw = (panelW - 2 * gap) / 3;
        addRenderableWidget(Button.builder(panelLabel(), b -> {
            VaultOddsClient.panelVisible = !VaultOddsClient.panelVisible;
            VaultOddsClient.save();
            b.setMessage(panelLabel());
        }).bounds(panelX, 38, bw, 20).build());
        addRenderableWidget(Button.builder(commonLabel(), b -> {
            VaultOddsClient.showCommon = !VaultOddsClient.showCommon;
            VaultOddsClient.save();
            b.setMessage(commonLabel());
        }).bounds(panelX + bw + gap, 38, bw, 20).build());
        addRenderableWidget(Button.builder(formatLabel(), b -> {
            VaultOddsClient.oddsFormat = VaultOddsClient.oddsFormat.next();
            VaultOddsClient.save();
            b.setMessage(formatLabel());
        }).bounds(panelX + 2 * (bw + gap), 38, bw, 20).build());
        addRenderableWidget(Button.builder(delayLabel(), b -> {
            VaultOddsClient.nextDelay();
            b.setMessage(delayLabel());
        }).bounds(panelX, 62, panelW, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(this.width / 2 - 50, this.height - 28, 100, 20).build());
    }

    private static Component panelLabel() {
        return Component.literal("Panel: " + (VaultOddsClient.panelVisible ? "ON" : "OFF"));
    }

    private static Component commonLabel() {
        return Component.literal("Common: " + (VaultOddsClient.showCommon ? "Shown" : "Hidden"));
    }

    private static Component delayLabel() {
        int t = VaultOddsClient.cooldownTicks;
        return Component.literal("Insert delay: " + t + (t <= 1 ? " tick" : " ticks"));
    }

    private static Component formatLabel() {
        return Component.literal("Odds: " + VaultOddsClient.oddsFormat.title);
    }

    private void centered(GuiGraphics g, String text, int y, int color) {
        g.drawString(this.font, text, this.width / 2 - this.font.width(text) / 2, y, color);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);

        int titleW = this.font.width("Vault Odds");
        g.renderItem(new ItemStack(Items.VAULT), this.width / 2 - titleW / 2 - 20, 9);
        centered(g, "Vault Odds", 13, 0xFFFFFFFF);
        centered(g, "Auto-insert settings", 24, DIM);

        centered(g, "\u26A0 Experimental: it is NOT sure this works", 90, WARN);
        centered(g, "The item shown is cosmetic, the server rolls the reward.", 102, WARN_SUB);

        g.drawString(this.font, "Insert the key when the vault shows:", panelX, dropY - 12, DIM);

        boolean hover = inside(mouseX, mouseY, panelX, dropY, panelW, 20);
        g.fill(panelX, dropY, panelX + panelW, dropY + 20, 0xFF101010);
        VaultPanel.outline(g, panelX, dropY, panelW, 20, (hover || expanded) ? GOLD : 0xFF707070);

        Target target = VaultOddsClient.target();
        int tx = panelX + 6;
        if (target.item() != null) {
            VaultPanel.smallItem(g, target.stack(), tx, dropY + 4);
            tx += 16;
        }
        g.drawString(this.font, target.label(), tx, dropY + 6, target.item() == null ? DIM : TEXT);
        String arrow = expanded ? "^" : "v";
        g.drawString(this.font, arrow, panelX + panelW - 6 - this.font.width(arrow), dropY + 6, DIM);

        if (expanded) {
            renderList(g, mouseX, mouseY);
        }
    }

    private void renderList(GuiGraphics g, int mouseX, int mouseY) {
        List<Target> targets = VaultOdds.TARGETS;
        Target current = VaultOddsClient.target();
        g.nextStratum();
        g.fill(panelX, listY, panelX + panelW, listY + listH, 0xF0101010);
        g.enableScissor(panelX, listY, panelX + panelW, listY + listH);
        for (int i = 0; i < targets.size(); i++) {
            int cx = panelX + (i % cols) * cellW;
            int cy = listY + (i / cols) * CELL_H - scroll;
            if (cy + CELL_H < listY || cy > listY + listH) continue;

            Target t = targets.get(i);
            boolean hover = inside(mouseX, mouseY, panelX, listY, panelW, listH)
                    && inside(mouseX, mouseY, cx, cy, cellW, CELL_H);
            if (hover) g.fill(cx, cy, cx + cellW, cy + CELL_H, 0x40FFFFFF);
            if (t == current) VaultPanel.outline(g, cx, cy, cellW, CELL_H, GOLD);
            if (t.item() != null) VaultPanel.smallItem(g, t.stack(), cx + 4, cy + 2);
            g.drawString(this.font, this.font.plainSubstrByWidth(t.label(), cellW - 26),
                    cx + 22, cy + 4, t.item() == null ? DIM : TEXT);
        }
        g.disableScissor();
        VaultPanel.outline(g, panelX, listY, panelW, listH, 0xFF707070);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int mx = (int) event.x();
        int my = (int) event.y();

        if (inside(mx, my, panelX, dropY, panelW, 20)) {
            expanded = !expanded;
            scroll = 0;
            return true;
        }
        if (expanded) {
            if (inside(mx, my, panelX, listY, panelW, listH)) {
                int col = Math.min((mx - panelX) / cellW, cols - 1);
                int row = (my - listY + scroll) / CELL_H;
                int index = row * cols + col;
                if (index >= 0 && index < VaultOdds.TARGETS.size()) {
                    VaultOddsClient.setTarget(index);
                }
            }
            expanded = false;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (expanded) {
            int rows = (VaultOdds.TARGETS.size() + cols - 1) / cols;
            int maxScroll = Math.max(0, rows * CELL_H - listH);
            scroll = Math.max(0, Math.min(maxScroll, scroll - (int) (scrollY * CELL_H)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
