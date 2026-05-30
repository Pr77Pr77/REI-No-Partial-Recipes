package de.pr77pr77.rei.nopartialrecipes;

import com.google.gson.*;
import net.minecraft.recipe.*;
import net.minecraft.registry.*;
import net.minecraft.resource.*;
import net.minecraft.util.Identifier;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class RecipeJsonDumper {
    public static List<RecipeData> recipes;

    public static class RecipeData {
        final Identifier id;
        final JsonElement json;

        RecipeData(Identifier id, JsonElement json) {
            this.id = id;
            this.json = json;
        }
    }

    public static void init() {
        List<ResourcePack> packs = new ArrayList<>();

        packs.add(VanillaDataPackProvider.createDefaultPack());

        LifecycledResourceManagerImpl resourceManager = new LifecycledResourceManagerImpl(
                ResourceType.SERVER_DATA,
                packs
        );

        loadRecipes(resourceManager);
        resourceManager.close();
    }

    private static void loadRecipes(ResourceManager resourceManager) {
        recipes = new ArrayList<>();

        Map<Identifier, Resource> resources = resourceManager.findResources(
                "recipe",
                id -> id.getNamespace().equals("minecraft") && id.getPath().endsWith(".json")
        );

        for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
            try (InputStream is = entry.getValue().getInputStream();
                 InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {

                JsonElement json = JsonParser.parseReader(reader);

                String path = entry.getKey().getPath();
                String name = path
                        .substring("recipe/".length())
                        .replaceAll("\\.json$", "");

                recipes.add(new RecipeData(Identifier.ofVanilla(name), json));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}