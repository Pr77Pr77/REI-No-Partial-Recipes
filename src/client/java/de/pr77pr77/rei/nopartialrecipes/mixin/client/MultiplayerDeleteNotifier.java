package de.pr77pr77.rei.nopartialrecipes.mixin.client;

import de.pr77pr77.rei.nopartialrecipes.REINoPartialRecipesClient;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiplayerScreen.class)
public class MultiplayerDeleteNotifier {
    @Inject(method = "removeEntry", at = @At("TAIL"))
    private void nopartialrecipes$cleanupJSONonServerDeletion(boolean confirmedAction, CallbackInfo ci) {
        REINoPartialRecipesClient.serverManager.cleanup();
    }
}
