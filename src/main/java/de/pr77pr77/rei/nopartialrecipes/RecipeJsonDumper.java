package de.pr77pr77.rei.nopartialrecipes;

import com.google.gson.*;
import net.minecraft.recipe.*;
import net.minecraft.registry.*;
import net.minecraft.resource.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipFile;

import static com.google.gson.JsonParser.parseReader;

public final class RecipeJsonDumper {
    public static List<RecipeData> recipes;

    public static class RecipeData{
        Identifier id;
        JsonElement json;

        RecipeData(Identifier id, JsonElement json){
            this.id = id;
            this.json = json;
        }
    }

    public static void init() {
        loadRecipes();
    }

    public static File getJar(Class<?> clazz) {
        String path = clazz.getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .getPath();
        return new File(path);
    }

    private static void loadRecipes() {
        recipes = new ArrayList<>();
        try (ZipFile zip = new ZipFile(getJar(MinecraftServer.class))) {
            zip.stream()
                    .filter(e -> e.getName().startsWith("data/minecraft/recipe/") && e.getName().endsWith(".json"))
                    .forEach(e -> {
                        try (InputStream is = zip.getInputStream(e);
                             InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                            JsonElement json = parseReader(reader);

                            String path = e.getName();

                            int lastSlash = path.lastIndexOf('/');
                            String filename = (lastSlash >= 0) ? path.substring(lastSlash + 1) : path;

                            if (filename.endsWith(".json")) {
                                filename = filename.substring(0, filename.length() - 5);
                            }

                            recipes.add(new RecipeData(Identifier.of("minecraft", filename), json));
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}