package vaultodds;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Données statiques : tables de probabilités des vaults et liste des cibles. */
public final class VaultOdds {
    private VaultOdds() {}

    public enum Tier {
        UNIQUE("Unique", 0xFFFFAA00),
        RARE("Rare", 0xFF55FFFF),
        COMMON("Common", 0xFFAAAAAA);

        public final String title;
        public final int color;

        Tier(String title, int color) {
            this.title = title;
            this.color = color;
        }
    }

    public enum OddsFormat {
        PERCENT("Percent"),
        ONE_IN("1 in N"),
        BOTH("Both");

        public final String title;

        OddsFormat(String title) {
            this.title = title;
        }

        public OddsFormat next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    public record Entry(Item item, String label, double percent, Tier tier) {
        public ItemStack stack() {
            return new ItemStack(item);
        }

        public String percentText() {
            return String.format(Locale.ROOT, "%.1f%%", percent);
        }

        public String oneInText() {
            return "1/" + Math.max(1L, Math.round(100.0 / percent));
        }
    }

    public record Target(Item item, String label) {
        public ItemStack stack() {
            return item == null ? ItemStack.EMPTY : new ItemStack(item);
        }
    }

    private static Entry e(Item item, String label, double percent, Tier tier) {
        return new Entry(item, label, percent, tier);
    }

    static List<Entry> sorted(List<Entry> entries) {
        return entries.stream()
                .sorted(Comparator.comparing(Entry::tier).thenComparingDouble(Entry::percent))
                .toList();
    }

    public static final List<Entry> OMINOUS = sorted(List.of(
            e(Items.HEAVY_CORE, "Heavy Core", 7.5, Tier.UNIQUE),
            e(Items.MUSIC_DISC_CREATOR, "Music Disc (Creator)", 7.5, Tier.UNIQUE),
            e(Items.FLOW_BANNER_PATTERN, "Flow Banner Pattern", 15.0, Tier.UNIQUE),
            e(Items.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE, "Flow Armor Trim", 22.5, Tier.UNIQUE),
            e(Items.ENCHANTED_GOLDEN_APPLE, "Enchanted Golden Apple", 22.5, Tier.UNIQUE),
            e(Items.DIAMOND_BLOCK, "Block of Diamond", 2.76, Tier.RARE),
            e(Items.ENCHANTED_BOOK, "Book: Wind Burst", 5.52, Tier.RARE),
            e(Items.ENCHANTED_BOOK, "Book: Breach / Density", 5.52, Tier.RARE),
            e(Items.ENCHANTED_BOOK, "Book: Looting / Smite...", 5.52, Tier.RARE),
            e(Items.DIAMOND_CHESTPLATE, "Diamond Chestplate", 8.28, Tier.RARE),
            e(Items.DIAMOND_AXE, "Diamond Axe", 8.28, Tier.RARE),
            e(Items.GOLDEN_APPLE, "Golden Apple", 8.28, Tier.RARE),
            e(Items.CROSSBOW, "Crossbow", 11.03, Tier.RARE),
            e(Items.IRON_BLOCK, "Block of Iron", 11.03, Tier.RARE),
            e(Items.EMERALD_BLOCK, "Block of Emerald", 13.79, Tier.RARE),
            e(Items.OMINOUS_BOTTLE, "Ominous Bottle III-V", 13.91, Tier.COMMON),
            e(Items.DIAMOND, "Diamonds x2-3", 26.39, Tier.COMMON),
            e(Items.TIPPED_ARROW, "Arrows of Slowness IV", 37.54, Tier.COMMON),
            e(Items.WIND_CHARGE, "Wind Charges", 47.44, Tier.COMMON),
            e(Items.EMERALD, "Emeralds", 56.21, Tier.COMMON)));

    public static final List<Entry> NORMAL = sorted(List.of(
            e(Items.TRIDENT, "Trident", 2.08, Tier.UNIQUE),
            e(Items.MUSIC_DISC_PRECIPICE, "Music Disc (Precipice)", 4.17, Tier.UNIQUE),
            e(Items.GUSTER_BANNER_PATTERN, "Guster Banner Pattern", 4.17, Tier.UNIQUE),
            e(Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE, "Bolt Armor Trim", 6.25, Tier.UNIQUE),
            e(Items.GOLDEN_APPLE, "Golden Apple", 8.33, Tier.UNIQUE),
            e(Items.DIAMOND_CHESTPLATE, "Diamond Chestplate", 3.48, Tier.RARE),
            e(Items.DIAMOND_AXE, "Diamond Axe", 3.48, Tier.RARE),
            e(Items.ENCHANTED_BOOK, "Book: Mending / Riptide...", 6.96, Tier.RARE),
            e(Items.ENCHANTED_BOOK, "Book: Sharpness / Fortune...", 6.96, Tier.RARE),
            e(Items.CROSSBOW, "Crossbow", 6.96, Tier.RARE),
            e(Items.IRON_CHESTPLATE, "Iron Chestplate", 6.96, Tier.RARE),
            e(Items.IRON_AXE, "Iron Axe", 6.96, Tier.RARE),
            e(Items.GOLDEN_CARROT, "Golden Carrots", 6.96, Tier.RARE),
            e(Items.BOW, "Bow", 10.43, Tier.RARE),
            e(Items.SHIELD, "Damaged Shield", 10.43, Tier.RARE),
            e(Items.DIAMOND, "Diamonds x1-2", 8.53, Tier.COMMON),
            e(Items.OMINOUS_BOTTLE, "Ominous Bottle I-II", 16.52, Tier.COMMON),
            e(Items.IRON_INGOT, "Iron Ingots", 24.01, Tier.COMMON),
            e(Items.HONEY_BOTTLE, "Honey Bottles", 24.01, Tier.COMMON),
            e(Items.TIPPED_ARROW, "Arrows of Poison", 31.0, Tier.COMMON),
            e(Items.ARROW, "Arrows", 31.0, Tier.COMMON),
            e(Items.WIND_CHARGE, "Wind Charges", 31.0, Tier.COMMON),
            e(Items.EMERALD, "Emeralds", 38.44, Tier.COMMON)));

    public static final List<Target> TARGETS = buildTargets();

    private static String genericLabel(Entry entry) {
        Item item = entry.item();
        if (item == Items.ENCHANTED_BOOK) return "Enchanted Book";
        if (item == Items.TIPPED_ARROW) return "Tipped Arrows";
        if (item == Items.OMINOUS_BOTTLE) return "Ominous Bottle";
        if (item == Items.DIAMOND) return "Diamonds";
        return entry.label();
    }

    private static List<Target> buildTargets() {
        List<Entry> all = new ArrayList<>(OMINOUS);
        all.addAll(NORMAL);
        all.sort(Comparator.comparing(Entry::tier));

        Map<Item, String> byItem = new LinkedHashMap<>();
        for (Entry entry : all) {
            byItem.putIfAbsent(entry.item(), genericLabel(entry));
        }

        List<Target> out = new ArrayList<>();
        out.add(new Target(null, "Off"));
        byItem.forEach((item, label) -> out.add(new Target(item, label)));
        return List.copyOf(out);
    }
}
