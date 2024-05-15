package net.minecraft.data.recipes;

import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.Map;
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

public class SmithingTransformRecipeBuilder {
   private final Ingredient template;
   private final Ingredient base;
   private final Ingredient addition;
   private final RecipeCategory category;
   private final Item result;
   private final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();
   private final RecipeSerializer<?> type;

   public SmithingTransformRecipeBuilder(RecipeSerializer<?> pType, Ingredient pTemplate, Ingredient pBase, Ingredient pAddition, RecipeCategory pCategory, Item pResult) {
      this.category = pCategory;
      this.type = pType;
      this.template = pTemplate;
      this.base = pBase;
      this.addition = pAddition;
      this.result = pResult;
   }

   public static SmithingTransformRecipeBuilder smithing(Ingredient pTemplate, Ingredient pBase, Ingredient pAddition, RecipeCategory pCategory, Item pResult) {
      return new SmithingTransformRecipeBuilder(RecipeSerializer.SMITHING_TRANSFORM, pTemplate, pBase, pAddition, pCategory, pResult);
   }

   public SmithingTransformRecipeBuilder unlocks(String pKey, Criterion<?> pCriterion) {
      this.criteria.put(pKey, pCriterion);
      return this;
   }

   public void save(RecipeOutput pRecipeOutput, String pRecipeId) {
      this.save(pRecipeOutput, new ResourceLocation(pRecipeId));
   }

   public void save(RecipeOutput pRecipeOutput, ResourceLocation pRecipeId) {
      this.ensureValid(pRecipeId);
      Advancement.Builder advancement$builder = pRecipeOutput.advancement().addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(pRecipeId)).rewards(AdvancementRewards.Builder.recipe(pRecipeId)).requirements(AdvancementRequirements.Strategy.OR);
      this.criteria.forEach(advancement$builder::addCriterion);
      pRecipeOutput.accept(new SmithingTransformRecipeBuilder.Result(pRecipeId, this.type, this.template, this.base, this.addition, this.result, advancement$builder.build(pRecipeId.withPrefix("recipes/" + this.category.getFolderName() + "/"))));
   }

   private void ensureValid(ResourceLocation pLocation) {
      if (this.criteria.isEmpty()) {
         throw new IllegalStateException("No way of obtaining recipe " + pLocation);
      }
   }

   public static record Result(ResourceLocation id, RecipeSerializer<?> type, Ingredient template, Ingredient base, Ingredient addition, Item result, AdvancementHolder advancement) implements FinishedRecipe {
      public void serializeRecipeData(JsonObject p_266713_) {
         p_266713_.add("template", this.template.toJson(true));
         p_266713_.add("base", this.base.toJson(true));
         p_266713_.add("addition", this.addition.toJson(true));
         JsonObject jsonobject = new JsonObject();
         jsonobject.addProperty("item", BuiltInRegistries.ITEM.getKey(this.result).toString());
         p_266713_.add("result", jsonobject);
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