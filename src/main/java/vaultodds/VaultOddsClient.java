package vaultodds;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.VaultBlock;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;
import vaultodds.VaultOdds.OddsFormat;
import vaultodds.VaultOdds.Target;

/** État du mod, touches, configuration et logique d'auto-insertion de la clé. */
public final class VaultOddsClient {
    private VaultOddsClient() {}

    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("vaultodds", "main"));
    public static final KeyMapping TOGGLE_PANEL =
            new KeyMapping("key.vaultodds.toggle_panel", GLFW.GLFW_KEY_K, CATEGORY);
    public static final KeyMapping OPEN_MENU =
            new KeyMapping("key.vaultodds.open_menu", GLFW.GLFW_KEY_J, CATEGORY);

    public static boolean panelVisible = true;
    public static boolean showCommon = false;
    public static OddsFormat oddsFormat = OddsFormat.BOTH;
    public static int targetIndex = 0;
    /** Délai (en ticks, 20 = 1 s) entre deux insertions de clé. */
    public static int cooldownTicks = 2;
    private static final int[] DELAY_CHOICES = {0, 1, 2, 5, 10};

    private static final Set<BlockPos> USED_VAULTS = new HashSet<>();
    private static final Path CONFIG =
            FabricLoader.getInstance().getConfigDir().resolve("vault-odds.properties");
    private static int cooldown = 0;

    static {
        load();
    }

    /** Appelé par l'entrypoint client : force simplement le chargement de la classe. */
    public static void init() {}

    public static Target target() {
        int i = (int) Math.clamp((long) targetIndex, 0, VaultOdds.TARGETS.size() - 1);
        return VaultOdds.TARGETS.get(i);
    }

    public static void nextDelay() {
        int next = DELAY_CHOICES[0];
        for (int i = 0; i < DELAY_CHOICES.length; i++) {
            if (DELAY_CHOICES[i] == cooldownTicks) {
                next = DELAY_CHOICES[(i + 1) % DELAY_CHOICES.length];
                break;
            }
        }
        cooldownTicks = next;
        save();
    }

    public static void setTarget(int index) {
        targetIndex = index;
        USED_VAULTS.clear();
        save();
    }

    public static void onTick(Minecraft mc) {
        while (TOGGLE_PANEL.consumeClick()) {
            panelVisible = !panelVisible;
            save();
        }
        while (OPEN_MENU.consumeClick()) {
            if (mc.screen == null) {
                mc.setScreen(new TargetScreen(null));
            }
        }
        if (cooldown > 0) cooldown--;
        tryAutoInsert(mc);
    }

    /** Renvoie le coffre (vault) visé par le joueur, ou null. */
    public static BlockHitResult lookedAtVault(Minecraft mc) {
        if (mc.level != null
                && mc.hitResult instanceof BlockHitResult hit
                && hit.getType() == HitResult.Type.BLOCK
                && mc.level.getBlockState(hit.getBlockPos()).getBlock() instanceof VaultBlock) {
            return hit;
        }
        return null;
    }

    public static void tryAutoInsert(Minecraft mc) {
        Item wanted = target().item();
        if (wanted == null || cooldown > 0 || mc.player == null || mc.gameMode == null
                || mc.screen != null || mc.level == null) {
            return;
        }
        BlockHitResult hit = lookedAtVault(mc);
        if (hit == null) return;

        BlockPos pos = hit.getBlockPos();
        if (USED_VAULTS.contains(pos)) return;

        BlockState state = mc.level.getBlockState(pos);
        if (state.getValue(VaultBlock.STATE) != VaultState.ACTIVE) return;
        if (!(mc.level.getBlockEntity(pos) instanceof VaultBlockEntity vault)) return;
        if (!vault.getSharedData().getDisplayItem().is(wanted)) return;

        Item keyItem = state.getValue(VaultBlock.OMINOUS) ? Items.OMINOUS_TRIAL_KEY : Items.TRIAL_KEY;
        InteractionHand hand = findKey(mc.player, keyItem);
        if (hand == null) return;

        mc.gameMode.useItemOn(mc.player, hand, hit);
        mc.player.swing(hand);
        cooldown = cooldownTicks;
        USED_VAULTS.add(pos.immutable());

        mc.gui.getChat().addMessage(
                Component.literal("[Vault Odds] ").withStyle(ChatFormatting.GOLD)
                        .append(Component.literal("Key inserted on ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(target().label()).withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal(" (reward not guaranteed)")
                                .withStyle(ChatFormatting.DARK_GRAY)));
    }

    private static InteractionHand findKey(Player player, Item key) {
        if (player.getMainHandItem().is(key)) return InteractionHand.MAIN_HAND;
        if (player.getOffhandItem().is(key)) return InteractionHand.OFF_HAND;
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < 9; slot++) {
            if (inventory.getItem(slot).is(key)) {
                inventory.setSelectedSlot(slot);
                return InteractionHand.MAIN_HAND;
            }
        }
        return null;
    }

    public static void save() {
        Properties props = new Properties();
        props.setProperty("panelVisible", Boolean.toString(panelVisible));
        props.setProperty("showCommon", Boolean.toString(showCommon));
        props.setProperty("oddsFormat", oddsFormat.name());
        props.setProperty("cooldownTicks", Integer.toString(cooldownTicks));
        props.setProperty("target", target().label());
        try (Writer w = Files.newBufferedWriter(CONFIG)) {
            props.store(w, "Vault Odds settings");
        } catch (IOException ignored) {
            // pas grave : on perd juste la sauvegarde des réglages
        }
    }

    private static void load() {
        if (!Files.exists(CONFIG)) return;
        Properties props = new Properties();
        try (Reader r = Files.newBufferedReader(CONFIG)) {
            props.load(r);
        } catch (IOException e) {
            return;
        }
        panelVisible = Boolean.parseBoolean(props.getProperty("panelVisible", "true"));
        showCommon = Boolean.parseBoolean(props.getProperty("showCommon", "false"));
        try {
            oddsFormat = OddsFormat.valueOf(props.getProperty("oddsFormat", "BOTH"));
        } catch (IllegalArgumentException e) {
            oddsFormat = OddsFormat.BOTH;
        }
        try {
            cooldownTicks = Math.clamp(
                    Long.parseLong(props.getProperty("cooldownTicks", "2")), 0, 20);
        } catch (NumberFormatException e) {
            cooldownTicks = 2;
        }
        String saved = props.getProperty("target", "");
        for (int i = 0; i < VaultOdds.TARGETS.size(); i++) {
            if (VaultOdds.TARGETS.get(i).label().equals(saved)) {
                targetIndex = i;
                break;
            }
        }
    }
}
