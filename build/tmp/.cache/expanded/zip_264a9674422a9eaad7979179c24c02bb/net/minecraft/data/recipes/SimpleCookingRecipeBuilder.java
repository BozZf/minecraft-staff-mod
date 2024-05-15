package net.minecraft.data.recipes;

import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;

public class SimpleCookingRecipeBuilder implements RecipeBuilder {
   private final RecipeCategory category;
   private final CookingBookCategory bookCategory;
   private final Item result;
   private final Ingredient ingredient;
   private final float experience;
   private final int cookingTime;
   private final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();
   @Nullable
   private String group;
   private final RecipeSerializer<? extends AbstractCookingRecipe> serializer;

   private SimpleCookingRecipeBuilder(RecipeCategory pCategory, CookingBookCategory pBookCategory, ItemLike pResult, Ingredient pIngredient, float pExperience, int pCookingTime, RecipeSerializer<? extends AbstractCookingRecipe> pSerializer) {
      this.category = pCategory;
      this.bookCategory = pBookCategory;
      this.result = pResult.asItem();
      this.ingredient = pIngredient;
      this.experience = pExperience;
      this.cookingTime = pCookingTime;
      this.serializer = pSerializer;
   }

   public static SimpleCookingRecipeBuilder generic(Ingredient pIngredient, RecipeCategory pCategory, ItemLike pResult, float pExperience, int pCookingTime, RecipeSerializer<? extends AbstractCookingRecipe> pSerializer) {
      return new SimpleCookingRecipeBuilder(pCategory, determineRecipeCategory(pSerializer, pResult), pResult, pIngredient, pExperience, pCookingTime, pSerializer);
   }

   public static SimpleCookingRecipeBuilder campfireCooking(Ingredient pIngredient, RecipeCategory pCategory, ItemLike pResult, float pExperience, int pCookingTime) {
      return new SimpleCookingRecipeBuilder(pCategory, CookingBookCategory.FOOD, pResult, pIngredient, pExperience, pCookingTime, RecipeSerializer.CAMPFIRE_COOKING_RECIPE);
   }

   public static SimpleCookingRecipeBuilder blasting(Ingredient pIngredient, RecipeCategory pCategory, ItemLike pResult, float pExperience, int pCookingTime) {
      return new SimpleCookingRecipeBuilder(pCategory, determineBlastingRecipeCategory(pResult), pResult, pIngredient, pExperience, pCookingTime, RecipeSerializer.BLASTING_RECIPE);
   }

   public static SimpleCookingRecipeBuilder smelting(Ingredient pIngredient, RecipeCategory pCategory, ItemLike pResult, float pExperience, int pCookingTime) {
      return new SimpleCookingRecipeBuilder(pCategory, determineSmeltingRecipeCategory(pResult), pResult, pIngredient, pExperience, pCookingTime, RecipeSerializer.SMELTING_RECIPE);
   }

   public static SimpleCookingRecipeBuilder smoking(Ingredient pIngredient, RecipeCategory pCategory, ItemLike pResult, float pExperience, int pCookingTime) {
      return new SimpleCookingRecipeBuilder(pCategory, CookingBookCategory.FOOD, pResult, pIngredient, pExperience, pCookingTime, RecipeSerializer.SMOKING_RECIPE);
   }

   public SimpleCookingRecipeBuilder unlockedBy(String pName, Criterion<?> pCriterion) {
      this.criteria.put(pName, pCriterion);
      return this;
   }

   public SimpleCookingRecipeBuilder group(@Nullable String pGroupName) {
      this.group = pGroupName;
      return this;
   }

   public Item getResult() {
      return this.result;
   }

   public void save(RecipeOutput pRecipeOutput, ResourceLocation pId) {
      this.ensureValid(pId);
      Advancement.Builder advancement$builder = pRecipeOutput.advancement().addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(pId)).rewards(AdvancementRewards.Builder.recipe(pId)).requirements(AdvancementRequirements.Strategy.OR);
      this.criteria.forEach(advancement$builder::addCriterion);
      pRecipeOutput.accept(new SimpleCookingRecipeBuilder.Result(pId, this.group == null ? "" : this.group, this.bookCategory, this.ingredient, this.result, this.experience, this.cookingTime, advancement$builder.build(pId.withPrefix("recipes/" + this.category.getFolderName() + "/")), this.serializer, this.condition));
   }

   @Nullable
   private net.minecraftforge.common.crafting.conditions.ICondition condition;
   public SimpleCookingRecipeBuilder condition(net.minecraftforge.common.crafting.conditions.ICondition condition) {
       this.condition = condition;
       return this;
   }

   private static CookingBookCategory determineSmeltingRecipeCategory(ItemLike pResult) {
      if (pResult.asItem().isEdible()) {
         return CookingBookCategory.FOOD;
      } else {
         return pResult.asItem() instanceof BlockItem ? CookingBookCategory.BLOCKS : CookingBookCategory.MISC;
      }
   }

   private static CookingBookCategory determineBlastingRecipeCategory(ItemLike pResult) {
      return pResult.asItem() instanceof BlockItem ? CookingBookCategory.BLOCKS : CookingBookCategory.MISC;
   }

   private static CookingBookCategory determineRecipeCategory(RecipeSerializer<? extends AbstractCookingRecipe> pSerializer, ItemLike pResult) {
      if (pSerializer == RecipeSerializer.SMELTING_RECIPE) {
         return determineSmeltingRecipeCategory(pResult);
      } else if (pSerializer == RecipeSerializer.BLASTING_RECIPE) {
         return determineBlastingRecipeCategory(pResult);
      } else if (pSerializer != RecipeSerializer.SMOKING_RECIPE && pSerializer != RecipeSerializer.CAMPFIRE_COOKING_RECIPE) {
         throw new IllegalStateException("Unknown cooking recipe type");
      } else {
         return CookingBookCategory.FOOD;
      }
   }

   /**
    * Makes sure that this obtainable.
    */
   private void ensureValid(ResourceLocation pId) {
      if (this.criteria.isEmpty()) {
         throw new IllegalStateException("No way of obtaining recipe " + pId);
      }
   }

   static record Result(ResourceLocation id, String group, CookingBookCategory category, Ingredient ingredient, Item result, float experience, int cookingTime, AdvancementHolder advancement, RecipeSerializer<? extends AbstractCookingRecipe> type, net.minecraftforge.common.crafting.conditions.ICondition condition) implements FinishedRecipe {
      public Result(ResourceLocation id, String group, CookingBookCategory category, Ingredient ingredient, Item result, float experience, int cookingTime, AdvancementHolder advancement, RecipeSerializer<? extends AbstractCookingRecipe> type) {
          this(id, group, category, ingredient, result, experience, cookingTime, advancement, type, null);
      }

      public void serializeRecipeData(JsonObject pJson) {
         net.minecraftforge.common.ForgeHooks.writeCondition(this.condition, pJson);
         if (!this.group.isEmpty()) {
            pJson.addProperty("group", this.group);
         }

         pJson.addProperty("category", this.category.getSerializedName());
         pJson.add("ingredient", this.ingredient.toJson(false));
         pJson.addProperty("result", BuiltInRegistries.ITEM.getKey(this.result).toString());
         pJson.addProperty("experience", this.experience);
         pJson.addProperty("cookingtime", this.cookingTime);
      }

      public ResourceLocation id() {
         return this.id;
      }

      public AdvancementHolder advancement() {
         return this.advancement;
      }

      public RecipeSerializer<? extends AbstractCookingRecipe> type() {
         return this.type;
      }
   }
}
