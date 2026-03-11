package de.pr77pr77.rei.nopartialrecipes.mixin.client;

import net.minecraft.recipe.TransmuteRecipe;
import net.minecraft.recipe.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TransmuteRecipe.class)
public interface TransmuteRecipeAccessor {

    @Accessor("input")
    Ingredient getInput();

    @Accessor("material")
    Ingredient getMaterial();
}