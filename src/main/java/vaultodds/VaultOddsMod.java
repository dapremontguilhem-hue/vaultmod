package vaultodds;

import net.fabricmc.api.ClientModInitializer;

/** Point d'entrée volontairement minimal : tout le reste passe par les mixins. */
public class VaultOddsMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        VaultOddsClient.init();
    }
}
