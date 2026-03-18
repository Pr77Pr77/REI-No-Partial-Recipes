package de.pr77pr77.rei.nopartialrecipes.mixin.client;

import de.pr77pr77.rei.nopartialrecipes.REINoPartialRecipesClient;
import me.shedaniel.rei.impl.client.gui.config.REIConfigScreen;
import me.shedaniel.rei.impl.client.gui.config.options.CompositeOption;
import me.shedaniel.rei.impl.client.gui.config.options.OptionCategory;
import me.shedaniel.rei.impl.client.gui.config.options.OptionGroup;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static me.shedaniel.rei.impl.client.gui.config.options.ConfigUtils.translatable;

@Mixin(value = REIConfigScreen.class, remap = false)
public class REIConfigScreenAdder {
    @ModifyVariable(
            method = "<init>*",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private static List<OptionCategory> nopartialrecipes$modifyCategories(List<OptionCategory> original) {
        List<OptionCategory> modified = new ArrayList<>(original);

        OptionCategory optionCategory = OptionCategory.of("no-partial-recipes", Identifier.of("rei-no-partial-recipes:textures/gui/config-icon.png"),
                translatable("config.rei-no-partial-recipes.categories.no-partial-recipes"),
                translatable("config.rei-no-partial-recipes.categories.no-partial-recipes.desc"));

        OptionGroup optionGroup = new OptionGroup("no-partial-recipes.servers", translatable("config.rei-no-partial-recipes.options.groups.no-partial-recipes.servers"));

        ServerList list = new ServerList(MinecraftClient.getInstance());
        list.loadFile();

        ServerInfo server = MinecraftClient.getInstance().getCurrentServerEntry();

        for (int i = 0; i < list.size(); i++) {
            ServerInfo info = list.get(i);

            Text text;
            if (server != null && Objects.equals(info.address, server.address)) {
                text = Text.empty().append(
                                translatable("config.rei-no-partial-recipes.options.server.current").formatted(Formatting.AQUA))
                        .append(Text.literal(info.name))
                        .append(Text.literal(" (" + info.address + ")").formatted(Formatting.GRAY));
            } else {
                text = Text.literal(info.name).append(Text.literal(" (" + info.address + ")").formatted(Formatting.GRAY));
            }
            optionGroup.add(new CompositeOption<>("no-partial-recipes.server" + i,
                    text,
                    translatable("config.rei-no-partial-recipes.options.server.desc", info.name, info.address),
                    impl -> REINoPartialRecipesClient.serverManager.getServerRecipeDataIDs(info.address).contains("minecraft"),
                    (impl, value) -> REINoPartialRecipesClient.serverManager.setServer(info.address, value))
                    .enabledDisabled());
        }

        modified.add(
                optionCategory.add(optionGroup)
        );

        return modified;
    }
}
