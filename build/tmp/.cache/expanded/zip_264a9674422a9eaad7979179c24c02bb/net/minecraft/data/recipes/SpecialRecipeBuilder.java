package net.minecraft.data.recipes;

import javax.annotation.Nullable;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class SpecialRecipeBuilder extends CraftingRecipeBuilder {
   final RecipeSerializer<?> serializer;

   public SpecialRecipeBuilder(RecipeSerializer<?> pSerializer) {
      this.serializer = pSerializer;
   }

   public static SpecialRecipeBuilder special(RecipeSerializer<? extends CraftingRecipe> pSerializer) {
      return new SpecialRecipeBuilder(pSerializer);
   }

   public void save(RecipeOutput pRecipeOutput, String pRecipeId) {
      this.save(pRecipeOutput, new ResourceLocation(pRecipeId));
   }

   public void save(RecipeOutput pRecipeOutput, final ResourceLocation pRecipeId) {
      pRecipeOutput.accept(new CraftingRecipeBuilder.CraftingResult(CraftingBookCategory.MISC) {
         public RecipeSerializer<?> type() {
            return SpecialRecipeBuilder.this.serializer;
         }

         public ResourceLocation id() {
            return pRecipeId;
         }

         @Nullable
         public AdvancementHolder advancement() {
            return null;
         }
      });
   }
}