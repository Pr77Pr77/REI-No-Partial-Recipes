package de.pr77pr77.rei.nopartialrecipes;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import de.pr77pr77.rei.nopartialrecipes.mixin.client.TransmuteRecipeAccessor;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.plugin.client.categories.crafting.filler.ArmorDyeRecipeFiller;
import me.shedaniel.rei.plugin.common.displays.DefaultCampfireDisplay;
import me.shedaniel.rei.plugin.common.displays.DefaultSmithingDisplay;
import me.shedaniel.rei.plugin.common.displays.DefaultStoneCuttingDisplay;
import me.shedaniel.rei.plugin.common.displays.cooking.DefaultBlastingDisplay;
import me.shedaniel.rei.plugin.common.displays.cooking.DefaultSmeltingDisplay;
import me.shedaniel.rei.plugin.common.displays.cooking.DefaultSmokingDisplay;
import me.shedaniel.rei.plugin.common.displays.crafting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.recipe.*;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.recipe.input.SmithingRecipeInput;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.NonNull;

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

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        if (!serverManager.getCurrentServerRecipeDataIDs().contains("minecraft")) {
            return;
        }
        recipes.forEach((RecipeJsonDumper.RecipeData data) -> {
            Recipe<?> recipe = parseRecipeFromJson(data.id, data.json);
            // Crafting:
            if (recipe instanceof ShapedRecipe shapedCrafting) {
                registry.add(new DefaultShapedDisplay(new RecipeEntry<>(RegistryKey.of(RegistryKeys.RECIPE, data.id), shapedCrafting)));
            } else if (recipe instanceof ShapelessRecipe shapelessCrafting) {
                registry.add(new DefaultShapelessDisplay(new RecipeEntry<>(RegistryKey.of(RegistryKeys.RECIPE, data.id), shapelessCrafting)));
            } else if (recipe instanceof TransmuteRecipe transmuteRecipe) {
                if (MinecraftClient.getInstance().world == null) {
                    return;
                }
                Ingredient inputIng = ((TransmuteRecipeAccessor) transmuteRecipe).getInput();
                Ingredient materialIng = ((TransmuteRecipeAccessor) transmuteRecipe).getMaterial();

                ItemStack exampleInput = ItemStack.EMPTY;
                ItemStack exampleMaterial = ItemStack.EMPTY;

                for (Identifier id : Registries.ITEM.getIds()) {
                    Item item = Registries.ITEM.get(id);
                    ItemStack s = new ItemStack(item);
                    if (exampleInput.isEmpty() && inputIng.test(s)) {
                        exampleInput = s.copy();
                    }
                    if (exampleMaterial.isEmpty() && materialIng.test(s)) {
                        exampleMaterial = s.copy();
                    }
                    if (!exampleInput.isEmpty() && !exampleMaterial.isEmpty()) break;
                }

                if (exampleInput.isEmpty() || exampleMaterial.isEmpty()) {
                    LOGGER.warn("No valid item for TransmuteRecipe found: " + data.id);
                    return;
                }

                List<ItemStack> stacks = new ArrayList<>();
                stacks.add(exampleInput);
                stacks.add(exampleMaterial);
                CraftingRecipeInput craftInput = CraftingRecipeInput.create(2, 1, stacks);

                RegistryWrapper.WrapperLookup lookup = MinecraftClient.getInstance().world.getRegistryManager();

                ItemStack crafted = transmuteRecipe.craft(craftInput, lookup);

                if (!crafted.isEmpty()) {
                    EntryIngredient output = EntryIngredients.of(crafted);

                    registry.add(new DefaultCustomShapelessDisplay(List.of(EntryIngredients.ofIngredient(inputIng), EntryIngredients.ofIngredient(materialIng)), List.of(output), Optional.empty()));
                } else {
                    LOGGER.warn("TransmuteRecipe.craft(...) did not produce a result!");
                }

            } else if (recipe instanceof TippedArrowRecipe tippedArrowRecipe) {
                if (MinecraftClient.getInstance().world == null) {
                    return;
                }
                ItemStack arrow = new ItemStack(Items.ARROW);
                EntryIngredient arrowEntryIng = EntryIngredients.ofItems(List.of(Items.ARROW));

                for (Potion potion : Registries.POTION) {
                    ItemStack stack = new ItemStack(Items.LINGERING_POTION);
                    RegistryEntry<Potion> entry = Registries.POTION.getEntry(potion);
                    stack.set(
                            DataComponentTypes.POTION_CONTENTS,
                            new PotionContentsComponent(entry)
                    );

                    CraftingRecipeInput craftInput = CraftingRecipeInput.create(3, 3, List.of(
                            arrow, arrow, arrow,
                            arrow, stack, arrow,
                            arrow, arrow, arrow
                    ));
                    RegistryWrapper.WrapperLookup lookup = MinecraftClient.getInstance().world.getRegistryManager();
                    ItemStack crafted = tippedArrowRecipe.craft(craftInput, lookup);
                    if (!crafted.isEmpty()) {
                        registry.add(new DefaultCustomDisplay(List.of(
                                arrowEntryIng, arrowEntryIng, arrowEntryIng,
                                arrowEntryIng, EntryIngredients.ofItemStacks(List.of(stack)), arrowEntryIng,
                                arrowEntryIng, arrowEntryIng, arrowEntryIng),
                                List.of(EntryIngredients.ofItemStacks(List.of(crafted))), Optional.empty()));
                    } else {
                        LOGGER.warn("TippedArrowRecipe.craft(...) did not produce a result for potion: " + potion.getBaseName());
                    }
                }
            } else if (recipe instanceof MapCloningRecipe mapCloningRecipe) {
                if (MinecraftClient.getInstance().world == null) {
                    return;
                }
                ItemStack map = new ItemStack(Items.FILLED_MAP);
                map.set(DataComponentTypes.MAP_ID, new MapIdComponent(0));

                CraftingRecipeInput craftInput = CraftingRecipeInput.create(2, 1, List.of(
                        map, new ItemStack(Items.MAP)
                ));
                RegistryWrapper.WrapperLookup lookup = MinecraftClient.getInstance().world.getRegistryManager();
                ItemStack crafted = mapCloningRecipe.craft(craftInput, lookup);
                if (!crafted.isEmpty()) {
                    registry.add(new DefaultCustomShapelessDisplay(List.of(
                            EntryIngredients.ofItemStacks(List.of(map)), EntryIngredients.ofItems(List.of(Items.MAP))),
                            List.of(EntryIngredients.ofItemStacks(List.of(crafted))), Optional.empty()));
                } else {
                    LOGGER.warn("MapCloningRecipe.craft(...) did not produce a result!");
                }
            } else if (recipe instanceof BookCloningRecipe bookCloningRecipe) {
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
                        LOGGER.warn("BookCloningRecipe.craft(...) did not produce a result for a book with book count: " + countEmptyBooks);
                    }
                }
            } else if (recipe instanceof ArmorDyeRecipe armorDyeRecipe) {
                ArmorDyeRecipeFiller armorDyeRecipeFiller = new ArmorDyeRecipeFiller();
                Collection<Display> displays = armorDyeRecipeFiller.apply(new RecipeEntry<>(RegistryKey.of(RegistryKeys.RECIPE, data.id), armorDyeRecipe));
                for (Display display : displays) {
                    registry.add(display);
                }
            }
            // Cooking:
            else if (recipe instanceof SmeltingRecipe smeltingRecipe) {
                registry.add(new DefaultSmeltingDisplay(new RecipeEntry<>(RegistryKey.of(RegistryKeys.RECIPE, data.id), (smeltingRecipe))));
            } else if (recipe instanceof SmokingRecipe smokingRecipe) {
                registry.add(new DefaultSmokingDisplay(new RecipeEntry<>(RegistryKey.of(RegistryKeys.RECIPE, data.id), smokingRecipe)));
            } else if (recipe instanceof CampfireCookingRecipe campfireCookingRecipe) {
                registry.add(new DefaultCampfireDisplay(new RecipeEntry<>(RegistryKey.of(RegistryKeys.RECIPE, data.id), (campfireCookingRecipe))));
            } else if (recipe instanceof BlastingRecipe blastingRecipe) {
                registry.add(new DefaultBlastingDisplay(new RecipeEntry<>(RegistryKey.of(RegistryKeys.RECIPE, data.id), (blastingRecipe))));
            }
            // Smithing:
            else if (recipe instanceof SmithingRecipe smithingRecipe) {
                if (MinecraftClient.getInstance() == null || MinecraftClient.getInstance().world == null) {
                    return;
                }

                // Getting inputs
                List<EntryIngredient> inputs = new ArrayList<>();
                if (smithingRecipe.template().isPresent()) {
                    inputs.add(EntryIngredients.ofIngredient(smithingRecipe.template().get()));
                }

                inputs.add(null);

                if (smithingRecipe.addition().isPresent()) {
                    inputs.add(EntryIngredients.ofIngredient(smithingRecipe.addition().get()));
                }

                // Getting output
                ItemStack templateStack;
                if (smithingRecipe.template().isPresent()) {
                    templateStack = Registries.ITEM.stream()
                            .map(ItemStack::new)
                            .filter(stack -> smithingRecipe.template().get().test(stack))
                            .findFirst()
                            .orElse(ItemStack.EMPTY);
                } else {
                    templateStack = ItemStack.EMPTY;
                }

                Registries.ITEM.stream()
                        .map(ItemStack::new)
                        .filter(stack -> smithingRecipe.base().test(stack))
                        .forEach(baseStack -> {
                            List<EntryIngredient> inputsSpecific = new ArrayList<>(inputs);
                            inputsSpecific.set(1, EntryIngredients.of(baseStack));

                            List<ItemStack> results = new ArrayList<>();

                            if (smithingRecipe.addition().isPresent()) {
                                Registries.ITEM.stream()
                                        .map(ItemStack::new)
                                        .filter(stack -> smithingRecipe.addition().get().test(stack))
                                        .forEach(additionStack -> {

                                            SmithingRecipeInput input = new SmithingRecipeInput(
                                                    templateStack,
                                                    baseStack,
                                                    additionStack
                                            );

                                            ItemStack result = smithingRecipe.craft(
                                                    input,
                                                    MinecraftClient.getInstance().world.getRegistryManager()
                                            );

                                            if (!result.isEmpty()) {
                                                results.add(result);
                                            }
                                        });
                            }

                            EntryIngredient output = EntryIngredients.ofItemStacks(results);

                            registry.add(new DefaultSmithingDisplay(
                                    inputsSpecific,
                                    List.of(output),
                                    Optional.of(data.id)
                            ));
                        });
            }
            // Stonecutting:
            else if (recipe instanceof StonecuttingRecipe stonecuttingRecipe) {
                registry.add(new DefaultStoneCuttingDisplay(new RecipeEntry<>(RegistryKey.of(RegistryKeys.RECIPE, data.id), stonecuttingRecipe)));
            } else if (!Objects.equals(data.id, Identifier.of("minecraft", "banner_duplicate")) && // REI doesn't display this in singleplayer.
                    !Objects.equals(data.id, Identifier.of("minecraft", "firework_star_fade")) && // REI doesn't display this in singleplayer.
                    !Objects.equals(data.id, Identifier.of("minecraft", "firework_star")) && // REI doesn't display this in singleplayer.
                    !Objects.equals(data.id, Identifier.of("minecraft", "firework_rocket")) && // REI doesn't display this in singleplayer.
                    !Objects.equals(data.id, Identifier.of("minecraft", "repair_item")) && // Already displayed by REI
                    !Objects.equals(data.id, Identifier.of("minecraft", "shield_decoration")) && // REI doesn't display this in singleplayer.
                    !Objects.equals(data.id, Identifier.of("minecraft", "decorated_pot"))) { // REI doesn't display this in singleplayer.
                LOGGER.warn("Dropped " + recipe.getType() + " recipe: " + data.id);
            }
        });
    }

    public static Recipe<?> parseRecipeFromJson(Identifier id, JsonElement json) {
        if (id == null) throw new IllegalArgumentException("id must not be null");
        if (json == null || !json.isJsonObject())
            throw new IllegalArgumentException("json must be a non-null JsonObject");

        JsonObject obj = json.getAsJsonObject();
        if (!obj.has("type")) {
            throw new IllegalArgumentException("recipe json has no 'type' field (required). Recipe ID: " + id);
        }

        try {
            RegistryOps<JsonElement> ops = getJsonElementRegistryOps(id);

            // get the serializer from the JSON "type" field
            String typeStr = obj.get("type").getAsString();
            Identifier serializerId = Identifier.of(typeStr);
            RecipeSerializer<?> serializer = Registries.RECIPE_SERIALIZER.get(serializerId);
            if (serializer == null) {
                throw new IllegalStateException("Unknown RecipeSerializer: " + serializerId + " (parsing recipe " + id + ")");
            }

            // MapCodec -> Codec -> parse with RegistryOps
            MapCodec<? extends Recipe<?>> mapCodec = serializer.codec();
            Codec<? extends Recipe<?>> codec = mapCodec.codec();
            DataResult<? extends Recipe<?>> result = codec.parse(ops, json);

            return result.result().orElseThrow(() -> {
                String err = result.error().map(DataResult.Error::message).orElse("unknown codec error");
                return new RuntimeException("Failed to parse recipe " + id + ": " + err);
            });

        } catch (Exception e) {
            throw new RuntimeException("Error while parsing recipe " + id, e);
        }
    }

    private static @NonNull RegistryOps<JsonElement> getJsonElementRegistryOps(Identifier id) {
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