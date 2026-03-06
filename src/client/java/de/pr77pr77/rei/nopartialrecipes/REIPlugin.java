package de.pr77pr77.rei.nopartialrecipes;

import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;

import static de.pr77pr77.rei.nopartialrecipes.REINoPartialRecipes.LOGGER;

public class REIPlugin implements REIClientPlugin {

    @Override
    public void registerScreens(ScreenRegistry registry) {
        LOGGER.info("Register Screens");
    }

}