package de.pr77pr77.rei.nopartialrecipes;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.display.reason.DisplayAdditionReason;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.registry.display.ServerDisplayRegistry;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.impl.client.registry.display.DisplayRegistryImpl;
import me.shedaniel.rei.impl.common.registry.displays.AbstractDisplayRegistry;
import me.shedaniel.rei.plugin.client.displays.ClientsidedCookingDisplay;
import me.shedaniel.rei.plugin.common.displays.cooking.DefaultBlastingDisplay;
import me.shedaniel.rei.plugin.common.displays.cooking.DefaultSmeltingDisplay;
import me.shedaniel.rei.plugin.common.displays.cooking.DefaultSmokingDisplay;
import me.shedaniel.rei.plugin.common.displays.crafting.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.*;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.*;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.util.Identifier;

import java.util.*;

import static de.pr77pr77.rei.nopartialrecipes.REINoPartialRecipes.LOGGER;
import static de.pr77pr77.rei.nopartialrecipes.REINoPartialRecipesClient.serverManager;
import static de.pr77pr77.rei.nopartialrecipes.RecipeJsonDumper.recipes;

public class REIPlugin implements REIClientPlugin {
    private static final String[] bookFillerTitles = new String[]{
            "Adventurer's Dreams", "Adventurer's Diary", "The Lost Journal",
            "The Lost Diary", "The Lost Book", "The Lost Tome", "The Lost Codex",
            "The Last Journal", "The Last Diary", "The Last Book", "The Last Tome",
            "Secrets of the World", "Secrets of the Universe", "Secrets of the Cosmos",
            "Myths of the World", "Myths of the Universe", "Myths of the Cosmos",
            "Old Tales of the World", "Old Tales of the Universe", "Old Tales of the Cosmos",
            "The World of the Legends", "The Universe of the Heroes", "The Cosmos of the Gods",
            "Diary of a Villager", "Diary of a Farmer", "Diary of a Fisherman",
            "Dungeon Journal", "Dungeon Diary", "Dungeon Book", "Dungeon Tome",
            "Why Is This Book Here?", "The Book of Nothing", "Definitely Not a Spellbook",
            "Secrets You Won't Find", "Definitely Important"
    };
    private static final String[] bookFillerAuthors = new String[]{
            "shedaniel", "Pr77Pr77", "AliBa2468", "Steve", "Alex", "Notch",
            "Herobrine", "God", "Santa Claus", "The Easter Bunny", "The Tooth Fairy"
    };

    public static REIPlugin instance;

    public boolean serverRecipesRegistered = false;
    public boolean vanillaRecipesRegistered = false;

    public REIPlugin() {
        instance = this;
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reconnectHandler());
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> reconnectHandler());
    }

    private void reconnectHandler(){
        serverRecipesRegistered = false;
        vanillaRecipesRegistered = false;
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void registerDisplays(DisplayRegistry registry) {
        if (!serverManager.getCurrentServerRecipeDataIDs().contains("minecraft") || vanillaRecipesRegistered) {
            return;
        }
        ServerDisplayRegistry serverRegistry = ServerDisplayRegistry.getInstance();
        recipes.forEach((RecipeJsonDumper.RecipeData data) -> {
            Recipe<?> recipe = parseRecipeFromJson(data.id, data.json);

            if (recipe instanceof BookCloningRecipe bookCloningRecipe) { // Own book cloning display for modifying the Easter egg
                if (MinecraftClient.getInstance().world == null) {
                    return;
                }
                List<ItemStack> writtenBooks = new ArrayList<>();
                List<String> authorsToUse = new ArrayList<>(List.of(bookFillerAuthors)); // Ensuring every author is used.
                for (String title : bookFillerTitles) { // There need to be more titles than authors
                    if (authorsToUse.isEmpty()) {
                        authorsToUse.addAll(List.of(bookFillerAuthors));
                    }
                    int authorIndex = new Random().nextInt(authorsToUse.size());

                    ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
                    stack.set(DataComponentTypes.WRITTEN_BOOK_CONTENT, new WrittenBookContentComponent(new RawFilteredPair<>(title, Optional.empty()),
                            authorsToUse.remove(authorIndex), 0, List.of(), true));

                    writtenBooks.add(stack);
                }

                RegistryWrapper.WrapperLookup lookup = MinecraftClient.getInstance().world.getRegistryManager();
                for (int countEmptyBooks = 1; countEmptyBooks < 9; countEmptyBooks++) {
                    List<ItemStack> craftedBooks = new ArrayList<>();
                    List<ItemStack> emptyBooks = new ArrayList<>();
                    List<EntryIngredient> emptyBooksIngredient = new ArrayList<>();
                    for (int bookAddIndex = 0; bookAddIndex < countEmptyBooks; bookAddIndex++) {
                        emptyBooks.add(new ItemStack(Items.WRITABLE_BOOK));
                        emptyBooksIngredient.add(EntryIngredients.ofItems(List.of(Items.WRITABLE_BOOK)));
                    }

                    for (ItemStack writtenBook : writtenBooks) {
                        List<ItemStack> allBooks = new ArrayList<>(List.of(writtenBook));
                        allBooks.addAll(emptyBooks);

                        CraftingRecipeInput craftInput = CraftingRecipeInput.create(2, 1, allBooks);

                        ItemStack crafted = bookCloningRecipe.craft(craftInput, lookup);
                        if (!crafted.isEmpty()) {
                            craftedBooks.add(crafted);
                        }
                    }

                    if (!craftedBooks.isEmpty()) {
                        List<EntryIngredient> allBooksEntryIngredient = new ArrayList<>(List.of(EntryIngredients.ofItemStacks(writtenBooks)));
                        allBooksEntryIngredient.addAll(emptyBooksIngredient);
                        registry.add(new DefaultCustomShapelessDisplay(allBooksEntryIngredient,
                                List.of(EntryIngredients.ofItemStacks(craftedBooks)), Optional.empty()));
                    } else {
                        LOGGER.warn("BookCloningRecipe.craft(...) did not produce a result for a book with book count: {}", countEmptyBooks);
                    }
                }
            } else if (recipe != null) { // Using the ServerDisplayRegistry to fill the displays from RecipeEntry.
                RecipeEntry<Recipe<?>> recipeEntry = new RecipeEntry<>(RegistryKey.of(RegistryKeys.RECIPE, data.id), recipe);
                for (Display display : serverRegistry.tryFillDisplay(recipeEntry, DisplayAdditionReason.RECIPE_MANAGER)) {
                    registry.add(display, recipeEntry);
                }
            }
        });
        if (serverRecipesRegistered) {
            List<Display> cookingDisplays = new ArrayList<>(registry.getAll().get(CategoryIdentifier.of("minecraft:plugins/smelting")));
            cookingDisplays.addAll(registry.getAll().get(CategoryIdentifier.of("minecraft:plugins/smoking")));
            cookingDisplays.addAll(registry.getAll().get(CategoryIdentifier.of("minecraft:plugins/blasting")));

            cookingDisplays.forEach((display) -> {
                if (removeServerRecipe((DisplayRegistryImpl) registry, display)) {
                    ((AbstractDisplayRegistry<?, ?>) registry).holder().remove(display);
                }
            });
        }
        vanillaRecipesRegistered = true;
    }

    public static boolean removeServerRecipe(DisplayRegistryImpl registry, Display display) {
        if (!(display instanceof ClientsidedCookingDisplay cookingDisplay)) {
            return false;
        }

        CategoryIdentifier<?> categoryIdentifier;
        Class<? extends Display> displayClass;

        switch (cookingDisplay) {
            case ClientsidedCookingDisplay.Smelting ignored -> {
                categoryIdentifier = CategoryIdentifier.of("minecraft:plugins/smelting");
                displayClass = DefaultSmeltingDisplay.class;
            }
            case ClientsidedCookingDisplay.Blasting ignored -> {
                categoryIdentifier = CategoryIdentifier.of("minecraft:plugins/blasting");
                displayClass = DefaultBlastingDisplay.class;
            }
            case ClientsidedCookingDisplay.Smoking ignored -> {
                categoryIdentifier = CategoryIdentifier.of("minecraft:plugins/smoking");
                displayClass = DefaultSmokingDisplay.class;
            }
            default -> {
                return false;
            }
        }

        return registry.getAll().get(categoryIdentifier).stream().anyMatch(existingDisplay ->
                displayClass.isInstance(existingDisplay) &&
                        existingDisplay.getInputEntries().equals(cookingDisplay.getInputEntries()) &&
                        existingDisplay.getOutputEntries().equals(cookingDisplay.getOutputEntries()));
    }

    public static Recipe<?> parseRecipeFromJson(Identifier id, JsonElement json) {
        if (id == null) throw new IllegalArgumentException("id must not be null");
        if (json == null || !json.isJsonObject())
            throw new IllegalArgumentException("json must be a non-null JsonObject");

        JsonObject obj = json.getAsJsonObject();
        if (!obj.has("type")) {
            LOGGER.error("Recipe json has no 'type' field (required). Recipe ID: {}", id);
            return null;
        }

        try {
            RegistryOps<JsonElement> ops = getJsonElementRegistryOps(id);

            // get the serializer from the JSON "type" field
            String typeStr = obj.get("type").getAsString();
            Identifier serializerId = Identifier.of(typeStr);
            RecipeSerializer<?> serializer = Registries.RECIPE_SERIALIZER.get(serializerId);
            if (serializer == null) {
                LOGGER.error("Unknown RecipeSerializer: {} (parsing recipe {})", serializerId, id);
                return null;
            }

            // MapCodec -> Codec -> parse with RegistryOps
            MapCodec<? extends Recipe<?>> mapCodec = serializer.codec();
            Codec<? extends Recipe<?>> codec = mapCodec.codec();
            DataResult<? extends Recipe<?>> result = codec.parse(ops, json);

            Optional<? extends Recipe<?>> optional = result.result();

            if (optional.isEmpty()) {
                String err = result.error()
                        .map(DataResult.Error::message)
                        .orElse("unknown codec error");

                if (err.contains("Not a JSON object")) {
                    LOGGER.warn("The server didn't send the required tag for the recipe '{}'. This recipe will not be available! Error message: {}", id, err);
                    return null;
                } else if (err.contains("ResourceKey[")) {
                    LOGGER.warn("The server didn't send the required registry entry for the recipe '{}'. This recipe will not be available! Error message: {}", id, err);
                    return null;
                } else if (err.contains("List is too short")) {
                    LOGGER.warn("The server did send an empty tag, which is required for the recipe '{}'. This recipe will not be available! Error message: {}", id, err);
                    return null;
                }
                LOGGER.error("Error while parsing recipe '{}': {}", id, err);
                return null;
            }

            return optional.get();

        } catch (Exception e) {
            String msg = e.getMessage();
            LOGGER.error("Error while or before parsing recipe '{}': {}", id, msg);
            return null;
        }
    }

    private static RegistryOps<JsonElement> getJsonElementRegistryOps(Identifier id) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayNetworkHandler handler = client.getNetworkHandler();

        DynamicRegistryManager drm;
        if (handler != null) {
            drm = handler.getRegistryManager();
        } else {
            throw new RuntimeException("Error while getting DynamicRegistryManager on recipe: " + id);
        }

        // Create RegistryOps for com.google.gson.JsonElement using JsonOps.INSTANCE
        return RegistryOps.of(JsonOps.INSTANCE, drm);
    }

}