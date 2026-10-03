package vaultodds;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.VaultBlock;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import vaultodds.VaultOdds.Entry;
import vaultodds.VaultOdds.OddsFormat;
import vaultodds.VaultOdds.Target;
import vaultodds.VaultOdds.Tier;

/** Panneau HUD affiché quand on regarde un vault. */
public final class VaultPanel {
    private VaultPanel() {}

    private static final int LINE = 0x30FFFFFF;
    private static final int SHOWN_BG = 0x38FFFFFF;
    private static final int TARGET_MARK = 0xFFFFD34D;
    private static final int DIM = 0xFF808080;

    public static void render(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (!VaultOddsClient.panelVisible || mc.player == null || mc.level == null
                || mc.options.hideGui || mc.screen != null) {
            return;
        }
        BlockHitResult hit = VaultOddsClient.lookedAtVault(mc);
        if (hit == null) return;

        var state = mc.level.getBlockState(hit.getBlockPos());
        boolean ominous = state.getValue(VaultBlock.OMINOUS);
        ItemStack shown = mc.level.getBlockEntity(hit.getBlockPos()) instanceof VaultBlockEntity vault
                ? vault.getSharedData().getDisplayItem()
                : ItemStack.EMPTY;

        List<Entry> entries = (ominous ? VaultOdds.OMINOUS : VaultOdds.NORMAL).stream()
                .filter(e -> VaultOddsClient.showCommon || e.tier() != Tier.COMMON)
                .toList();
        long tierCount = entries.stream().map(Entry::tier).distinct().count();
        double max = entries.stream().mapToDouble(Entry::percent).max().orElse(1.0);

        OddsFormat fmt = VaultOddsClient.oddsFormat;
        Font font = mc.font;
        int w = 196 + (fmt == OddsFormat.BOTH ? font.width("1/48") + 5 : 0);
        int h = 22 + (int) tierCount * 11 + entries.size() * 12 + 4 + 16;
        int x = g.guiWidth() - w - 14;
        int y = 14;

        g.nextStratum();
        g.fill(x - 4, y - 4, x + w + 4, y + h + 4, 0xF0100010);
        outline(g, x - 4, y - 4, w + 8, h + 8, 0xFF3A2A5A);

        g.renderItem(new ItemStack(Items.VAULT), x, y + 1);
        g.drawString(font, ominous ? "Ominous Vault" : "Vault", x + 20, y + 1,
                ominous ? 0xFFFF6B5A : 0xFFFFE066);
        g.drawString(font, "Chance per key", x + 20, y + 11, DIM);
        y += 22;

        Item targetItem = VaultOddsClient.target().item();
        Tier currentTier = null;
        for (Entry entry : entries) {
            if (entry.tier() != currentTier) {
                currentTier = entry.tier();
                drawSection(g, font, currentTier, x, y, w);
                y += 11;
            }
            drawRow(g, font, entry, x, y, w, fmt, max,
                    shown.is(entry.item()), entry.item() == targetItem);
            y += 12;
        }

        y += 3;
        g.fill(x, y, x + w, y + 1, LINE);
        y += 4;

        Target target = VaultOddsClient.target();
        if (target.item() == null) {
            g.drawString(font, "Auto-insert: off", x, y + 2, DIM);
        } else {
            g.drawString(font, "Auto-insert:", x, y + 2, DIM);
            int ix = x + font.width("Auto-insert: ");
            smallItem(g, target.stack(), ix, y);
            g.drawString(font, target.label(), ix + 13, y + 2, TARGET_MARK);
        }
    }

    private static void drawSection(GuiGraphics g, Font font, Tier tier, int x, int y, int w) {
        String title = tier.title.toUpperCase(java.util.Locale.ROOT);
        g.drawString(font, title, x, y + 1, tier.color, false);
        int lx = x + font.width(title) + 4;
        g.fill(lx, y + 5, x + w, y + 6, LINE);
    }

    private static void drawRow(GuiGraphics g, Font font, Entry entry, int x, int y, int w,
                                OddsFormat fmt, double max, boolean isShown, boolean isTarget) {
        if (isShown) g.fill(x - 2, y - 1, x + w + 2, y + 11, SHOWN_BG);
        if (isTarget) g.fill(x - 3, y - 1, x - 2, y + 11, TARGET_MARK);
        smallItem(g, entry.stack(), x, y - 1);

        int right = x + w;
        if (fmt != OddsFormat.PERCENT) {
            String t = entry.oneInText();
            int color = fmt == OddsFormat.BOTH ? DIM : entry.tier().color;
            g.drawString(font, t, right - font.width(t), y + 1, color);
            right -= font.width("1/48") + 5;
        }
        if (fmt != OddsFormat.ONE_IN) {
            String t = entry.percentText();
            g.drawString(font, t, right - font.width(t), y + 1, entry.tier().color);
            right -= font.width("56.2%") + 5;
        }

        int barX = right - 28;
        int barLen = Math.max(1, (int) Math.round(28 * entry.percent() / max));
        g.fill(barX, y + 4, barX + 28, y + 6, 0x40FFFFFF);
        g.fill(barX, y + 4, barX + barLen, y + 6, entry.tier().color);

        String label = font.plainSubstrByWidth(entry.label(), barX - (x + 14) - 4);
        g.drawString(font, label, x + 14, y + 1, isShown ? 0xFFFFFFFF : 0xFFE0E0E0);
    }

    /** Dessine un objet à 75 % de sa taille. */
    public static void smallItem(GuiGraphics g, ItemStack stack, int x, int y) {
        g.pose().pushMatrix();
        g.pose().translate((float) x, (float) y);
        g.pose().scale(0.75f, 0.75f);
        g.renderItem(stack, 0, 0);
        g.pose().popMatrix();
    }

    /** Cadre de 1 pixel. */
    public static void outline(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }
}
