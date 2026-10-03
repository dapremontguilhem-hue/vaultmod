package vaultodds.mixin;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import org.apache.commons.lang3.ArrayUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vaultodds.VaultOddsClient;

@Mixin(Options.class)
public class OptionsMixin {
    @Shadow
    @Final
    @Mutable
    public KeyMapping[] keyMappings;

    @Inject(method = "<init>",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;load()V"))
    private void vaultodds$addKeys(CallbackInfo ci) {
        this.keyMappings = ArrayUtils.addAll(this.keyMappings,
                VaultOddsClient.TOGGLE_PANEL, VaultOddsClient.OPEN_MENU);
    }
}
