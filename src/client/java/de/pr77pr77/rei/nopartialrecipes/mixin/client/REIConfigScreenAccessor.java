package de.pr77pr77.rei.nopartialrecipes.mixin.client;

import me.shedaniel.rei.impl.client.gui.config.REIConfigScreen;
import me.shedaniel.rei.impl.client.gui.config.options.OptionCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(REIConfigScreen.class)
public interface REIConfigScreenAccessor {

    @Accessor("activeCategory")
    void setActiveCategory(OptionCategory category);

    @Accessor("categories")
    List<OptionCategory> getCategories();

}