package de.pr77pr77.rei.nopartialrecipes;

import com.terraformersmc.modmenu.api.ModMenuApi;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import de.pr77pr77.rei.nopartialrecipes.mixin.client.REIConfigScreenAccessor;
import me.shedaniel.rei.impl.client.gui.config.REIConfigScreen;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {

        return parent -> {
            REIConfigScreen screen = new REIConfigScreen(parent);
            ((REIConfigScreenAccessor) screen).setActiveCategory(((REIConfigScreenAccessor) screen).getCategories().getLast());
            return screen;
        };

    }
}