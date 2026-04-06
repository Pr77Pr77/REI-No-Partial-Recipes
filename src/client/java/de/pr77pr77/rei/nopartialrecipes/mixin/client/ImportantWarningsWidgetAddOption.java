package de.pr77pr77.rei.nopartialrecipes.mixin.client;

import de.pr77pr77.rei.nopartialrecipes.REINoPartialRecipesClient;
import de.pr77pr77.rei.nopartialrecipes.REIPlugin;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

import static de.pr77pr77.rei.nopartialrecipes.REINoPartialRecipes.LOGGER;

@Mixin(me.shedaniel.rei.impl.client.gui.hints.ImportantWarningsWidget.class)
public abstract class ImportantWarningsWidgetAddOption {
    @Shadow(remap = false)
    @Final
    private Rectangle bounds;

    @Shadow(remap = false)
    private boolean visible;

    @Shadow(remap = false)
    private static boolean dirty;

    @Redirect(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;of(Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/List;"
            ),
            remap = false
    )
    private List<?> nopartialrecipes$addOption(Object title, Object text) {
        if (text instanceof Text realText) {
            if (REINoPartialRecipesClient.serverManager.relogRequired) {
                return List.of(Text.translatable("text.rei-no-partial-recepies.recipes.not.full.need.relog.title").formatted(Formatting.RED), Text.translatable("text.rei-no-partial-recepies.recipes.not.full.need.relog.desc"));
            } else {
                return List.of(title, realText.copy().append(Text.translatable("text.rei-no-partial-recepies.recipes.not.full.option").formatted(Formatting.GRAY)));
            }
        } else {
            return List.of(title, text);
        }
    }

    @Inject(
            method = "<init>",
            at = @At("TAIL"),
            remap = false
    )
    private void nopartialrecipes$makeInvisible(CallbackInfo ci) {
        if (REINoPartialRecipesClient.serverManager.getCurrentServerRecipeDataIDs().contains("minecraft") || MinecraftClient.getInstance().getCurrentServerEntry() == null) {
            this.visible = false;
        } else if(REINoPartialRecipesClient.serverManager.relogRequired) {
            this.visible = true;
        }
    }

    @Redirect(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/Math;min(II)I"
            ),
            remap = false
    )
    private int nopartialrecipes$addButtonSpace(int a, int b) {
        return REINoPartialRecipesClient.serverManager.relogRequired ? Math.min(a, b) : Math.min(a + 20, b);
    }

    @Unique
    private final Rectangle vanillaRecipesButtonBounds = new Rectangle();

    @Inject(
            method = "render",
            at = @At("TAIL")
    )
    private void nopartialrecipes$renderButton(DrawContext graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if(!REINoPartialRecipesClient.serverManager.relogRequired) {
            MutableText vanillaRecipesText = Text.translatable("text.rei-no-partial-recepies.recipes.not.full.option.button.enable");
            graphics.getMatrices().pushMatrix();
            graphics.getMatrices().translate(bounds.x + bounds.width / 2 - MinecraftClient.getInstance().textRenderer.getWidth(vanillaRecipesText) * 0.75f / 2, bounds.getMaxY() - 29);
            graphics.getMatrices().scale(0.75f, 0.75f);
            vanillaRecipesButtonBounds.setBounds(bounds.x, bounds.getMaxY() - 40, bounds.width, 19);
            graphics.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, vanillaRecipesText, 0, 0,
                    vanillaRecipesButtonBounds.contains(mouseX, mouseY) ? 0xfffff8de : 0xAAFFFFFF);
            graphics.getMatrices().popMatrix();
        }
    }

    @Inject(
            method = "mouseClicked",
            at = @At("RETURN"),
            cancellable = true)
    private void nopartialrecipes$checkButtonClicked(Click event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() && this.visible && event.button() == 0 && vanillaRecipesButtonBounds.contains(event.x(), event.y()) && !REINoPartialRecipesClient.serverManager.relogRequired) {
            dirty = false;
            this.visible = false;
            Widgets.produceClickSound();
            LOGGER.info("Clicked on enable vanilla recipes!");
            REINoPartialRecipesClient.serverManager.addCurrentServer();
            REIPlugin.instance.registerDisplays(DisplayRegistry.getInstance());
            cir.setReturnValue(true);
        }
    }
}