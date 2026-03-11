package de.pr77pr77.rei.nopartialrecipes;

import net.fabricmc.api.ClientModInitializer;

public class REINoPartialRecipesClient implements ClientModInitializer {
    public static ServerManager serverManager;

    @Override
    public void onInitializeClient() {
        // This entrypoint is suitable for setting up client-specific logic, such as rendering.
        serverManager = new ServerManager();
    }
}