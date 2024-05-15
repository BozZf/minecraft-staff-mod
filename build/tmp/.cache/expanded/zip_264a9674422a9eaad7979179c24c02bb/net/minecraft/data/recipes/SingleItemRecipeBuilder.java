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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;

public class SingleItemRecipeBuilder implements RecipeBuilder {
   private final RecipeCategory category;
   private final Item result;
   private final Ingredient ingredient;
   private final int count;
   private final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();
   @Nullable
   private String group;
   private final RecipeSerializer<?> type;

   public SingleItemRecipeBuilder(RecipeCategory pCategory, RecipeSerializer<?> pType, Ingredient pIngredient, ItemLike pResult, int pCount) {
      this.category = pCategory;
      this.type = pType;
      this.result = pResult.asItem();
      this.ingredient = pIngredient;
      this.count = pCount;
   }

   public static SingleItemRecipeBuilder stonecutting(Ingredient pIngredient, RecipeCategory pCategory, ItemLike pResult) {
      return new SingleItemRecipeBuilder(pCategory, RecipeSerializer.STONECUTTER, pIngredient, pResult, 1);
   }

   public static SingleItemRecipeBuilder stonecutting(Ingredient pIngredient, RecipeCategory pCategory, ItemLike pResult, int pCount) {
      return new SingleItemRecipeBuilder(pCategory, RecipeSerializer.STONECUTTER, pIngredient, pResult, pCount);
   }

   public SingleItemRecipeBuilder unlockedBy(String pName, Criterion<?> pCriterion) {
      this.criteria.put(pName, pCriterion);
      return this;
   }

   public SingleItemRecipeBuilder group(@Nullable String pGroupName) {
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
      pRecipeOutput.accept(new SingleItemRecipeBuilder.Result(pId, this.type, this.group == null ? "" : this.group, this.ingredient, this.result, this.count, advancement$builder.build(pId.withPrefix("recipes/" + this.category.getFolderName() + "/")), condition));
   }

   @Nullable
   private net.minecraftforge.common.crafting.conditions.ICondition condition;
   public SingleItemRecipeBuilder condition(net.minecraftforge.common.crafting.conditions.ICondition condition) {
       this.condition = condition;
       return this;
   }

   private void ensureValid(ResourceLocation pId) {
      if (this.criteria.isEmpty()) {
         throw new IllegalStateException("No way of obtaining recipe " + pId);
      }
   }

   public static record Result(ResourceLocation id, RecipeSerializer<?> type, String group, Ingredient ingredient, Item result, int count, AdvancementHolder advancement, net.minecraftforge.common.crafting.conditions.ICondition condition) implements FinishedRecipe {
      Result(ResourceLocation id, RecipeSerializer<?> type, String group, Ingredient ingredient, Item result, int count, AdvancementHolder advancement) {
        this(id, type, group, ingredient, result, count, advancement, null);
      }

      public void serializeRecipeData(JsonObject pJson) {
         net.minecraftforge.common.ForgeHooks.writeCondition(this.condition, pJson);
         if (!this.group.isEmpty()) {
            pJson.addProperty("group", this.group);
         }

         pJson.add("ingredient", this.ingredient.toJson(false));
         pJson.addProperty("result", BuiltInRegistries.ITEM.getKey(this.result).toString());
         pJson.addProperty("count", this.count);
      }

      public ResourceLocation id() {
         return this.id;
      }

      public RecipeSerializer<?> type() {
         return this.type;
      }

      public AdvancementHolder advancement() {
         return this.advancement;
      }
   }
}
