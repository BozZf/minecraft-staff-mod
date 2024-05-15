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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class SmithingTrimRecipeBuilder {
   private final RecipeCategory category;
   private final Ingredient template;
   private final Ingredient base;
   private final Ingredient addition;
   private final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();
   private final RecipeSerializer<?> type;

   public SmithingTrimRecipeBuilder(RecipeSerializer<?> pType, RecipeCategory pCategory, Ingredient pTemplate, Ingredient pBase, Ingredient pAddition) {
      this.category = pCategory;
      this.type = pType;
      this.template = pTemplate;
      this.base = pBase;
      this.addition = pAddition;
   }

   public static SmithingTrimRecipeBuilder smithingTrim(Ingredient pTemplate, Ingredient pBase, Ingredient pAddition, RecipeCategory pCategory) {
      return new SmithingTrimRecipeBuilder(RecipeSerializer.SMITHING_TRIM, pCategory, pTemplate, pBase, pAddition);
   }

   public SmithingTrimRecipeBuilder unlocks(String pKey, Criterion<?> pCriterion) {
      this.criteria.put(pKey, pCriterion);
      return this;
   }

   public void save(RecipeOutput pRecipeOutput, ResourceLocation pRecipeId) {
      this.ensureValid(pRecipeId);
      Advancement.Builder advancement$builder = pRecipeOutput.advancement().addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(pRecipeId)).rewards(AdvancementRewards.Builder.recipe(pRecipeId)).requirements(AdvancementRequirements.Strategy.OR);
      this.criteria.forEach(advancement$builder::addCriterion);
      pRecipeOutput.accept(new SmithingTrimRecipeBuilder.Result(pRecipeId, this.type, this.template, this.base, this.addition, advancement$builder.build(pRecipeId.withPrefix("recipes/" + this.category.getFolderName() + "/"))));
   }

   private void ensureValid(ResourceLocation pLocation) {
      if (this.criteria.isEmpty()) {
         throw new IllegalStateException("No way of obtaining recipe " + pLocation);
      }
   }

   public static record Result(ResourceLocation id, RecipeSerializer<?> type, Ingredient template, Ingredient base, Ingredient addition, AdvancementHolder advancement) implements FinishedRecipe {
      public void serializeRecipeData(JsonObject p_267008_) {
         p_267008_.add("template", this.template.toJson(true));
         p_267008_.add("base", this.base.toJson(true));
         p_267008_.add("addition", this.addition.toJson(true));
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