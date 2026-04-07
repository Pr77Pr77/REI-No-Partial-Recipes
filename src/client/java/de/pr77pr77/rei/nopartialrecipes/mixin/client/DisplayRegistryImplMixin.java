package de.pr77pr77.rei.nopartialrecipes.mixin.client;

import de.pr77pr77.rei.nopartialrecipes.REIPlugin;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.impl.client.registry.display.DisplayRegistryImpl;
import net.minecraft.recipe.RecipeDisplayEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

import static de.pr77pr77.rei.nopartialrecipes.REINoPartialRecipesClient.serverManager;

@Mixin(value = DisplayRegistryImpl.class, remap = false)
public abstract class DisplayRegistryImplMixin {

    @Redirect(
            method = "addRecipes",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/shedaniel/rei/impl/client/registry/display/DisplayRegistryImpl;add(Lme/shedaniel/rei/api/common/display/Display;Ljava/lang/Object;)Z"
            )
    )
    private boolean nopartialrecipes$blockExistingRecipes(DisplayRegistryImpl instance, Display display, Object origin) {
        if (serverManager.getCurrentServerRecipeDataIDs().contains("minecraft") && REIPlugin.removeServerRecipe(instance, display)) {
            return false;
        } else {
            return instance.add(display, origin);
        }
    }

    @Inject(
            method = "addRecipes",
            at = @At("TAIL")
    )
    private void nopartialrecipes$setServerRecipesRegistered(List<RecipeDisplayEntry> entries, CallbackInfo ci) {
        REIPlugin.instance.serverRecipesRegistered = true;
    }
}