package net.minecraft.data.recipes;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;

public class ShapedRecipeBuilder extends CraftingRecipeBuilder implements RecipeBuilder {
   private final RecipeCategory category;
   private final Item result;
   private final int count;
   private final List<String> rows = Lists.newArrayList();
   private final Map<Character, Ingredient> key = Maps.newLinkedHashMap();
   private final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();
   @Nullable
   private String group;
   private boolean showNotification = true;

   public ShapedRecipeBuilder(RecipeCategory pCategory, ItemLike pResult, int pCount) {
      this.category = pCategory;
      this.result = pResult.asItem();
      this.count = pCount;
   }

   /**
    * Creates a new builder for a shaped recipe.
    */
   public static ShapedRecipeBuilder shaped(RecipeCategory pCategory, ItemLike pResult) {
      return shaped(pCategory, pResult, 1);
   }

   /**
    * Creates a new builder for a shaped recipe.
    */
   public static ShapedRecipeBuilder shaped(RecipeCategory pCategory, ItemLike pResult, int pCount) {
      return new ShapedRecipeBuilder(pCategory, pResult, pCount);
   }

   /**
    * Adds a key to the recipe pattern.
    */
   public ShapedRecipeBuilder define(Character pSymbol, TagKey<Item> pTag) {
      return this.define(pSymbol, Ingredient.of(pTag));
   }

   /**
    * Adds a key to the recipe pattern.
    */
   public ShapedRecipeBuilder define(Character pSymbol, ItemLike pItem) {
      return this.define(pSymbol, Ingredient.of(pItem));
   }

   /**
    * Adds a key to the recipe pattern.
    */
   public ShapedRecipeBuilder define(Character pSymbol, Ingredient pIngredient) {
      if (this.key.containsKey(pSymbol)) {
         throw new IllegalArgumentException("Symbol '" + pSymbol + "' is already defined!");
      } else if (pSymbol == ' ') {
         throw new IllegalArgumentException("Symbol ' ' (whitespace) is reserved and cannot be defined");
      } else {
         this.key.put(pSymbol, pIngredient);
         return this;
      }
   }

   /**
    * Adds a new entry to the patterns for this recipe.
    */
   public ShapedRecipeBuilder pattern(String pPattern) {
      if (!this.rows.isEmpty() && pPattern.length() != this.rows.get(0).length()) {
         throw new IllegalArgumentException("Pattern must be the same width on every line!");
      } else {
         this.rows.add(pPattern);
         return this;
      }
   }

   public ShapedRecipeBuilder unlockedBy(String pName, Criterion<?> pCriterion) {
      this.criteria.put(pName, pCriterion);
      return this;
   }

   public ShapedRecipeBuilder group(@Nullable String pGroupName) {
      this.group = pGroupName;
      return this;
   }

   public ShapedRecipeBuilder showNotification(boolean pShowNotification) {
      this.showNotification = pShowNotification;
      return this;
   }

   public Item getResult() {
      return this.result;
   }

   public void save(RecipeOutput pRecipeOutput, ResourceLocation pId) {
      this.ensureValid(pId);
      Advancement.Builder advancement$builder = pRecipeOutput.advancement().addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(pId)).rewards(AdvancementRewards.Builder.recipe(pId)).requirements(AdvancementRequirements.Strategy.OR);
      this.criteria.forEach(advancement$builder::addCriterion);
      pRecipeOutput.accept(new ShapedRecipeBuilder.Result(pId, this.result, this.count, this.group == null ? "" : this.group, determineBookCategory(this.category), this.rows, this.key, advancement$builder.build(pId.withPrefix("recipes/" + this.category.getFolderName() + "/")), this.showNotification).withCondition(this.condition));
   }

   @Nullable
   private net.minecraftforge.common.crafting.conditions.ICondition condition;
   public ShapedRecipeBuilder condition(net.minecraftforge.common.crafting.conditions.ICondition condition) {
       this.condition = condition;
       return this;
   }

   /**
    * Makes sure that this recipe is valid and obtainable.
    */
   private void ensureValid(ResourceLocation pId) {
      if (this.rows.isEmpty()) {
         throw new IllegalStateException("No pattern is defined for shaped recipe " + pId + "!");
      } else {
         Set<Character> set = Sets.newHashSet(this.key.keySet());
         set.remove(' ');

         for(String s : this.rows) {
            for(int i = 0; i < s.length(); ++i) {
               char c0 = s.charAt(i);
               if (!this.key.containsKey(c0) && c0 != ' ') {
                  throw new IllegalStateException("Pattern in recipe " + pId + " uses undefined symbol '" + c0 + "'");
               }

               set.remove(c0);
            }
         }

         if (!set.isEmpty()) {
            throw new IllegalStateException("Ingredients are defined but not used in pattern for recipe " + pId);
         } else if (this.rows.size() == 1 && this.rows.get(0).length() == 1) {
            throw new IllegalStateException("Shaped recipe " + pId + " only takes in a single item - should it be a shapeless recipe instead?");
         } else if (this.criteria.isEmpty()) {
            throw new IllegalStateException("No way of obtaining recipe " + pId);
         }
      }
   }

   public static class Result extends CraftingRecipeBuilder.CraftingResult {
      private final ResourceLocation id;
      private final Item result;
      private final int count;
      private final String group;
      private final List<String> pattern;
      private final Map<Character, Ingredient> key;
      private final AdvancementHolder advancement;
      private final boolean showNotification;

      public Result(ResourceLocation pId, Item pResult, int pCount, String pGroup, CraftingBookCategory pCategory, List<String> pPattern, Map<Character, Ingredient> pKey, AdvancementHolder pAdvancement, boolean pShowNotification) {
         super(pCategory);
         this.id = pId;
         this.result = pResult;
         this.count = pCount;
         this.group = pGroup;
         this.pattern = pPattern;
         this.key = pKey;
         this.advancement = pAdvancement;
         this.showNotification = pShowNotification;
      }

      public void serializeRecipeData(JsonObject pJson) {
         super.serializeRecipeData(pJson);
         if (!this.group.isEmpty()) {
            pJson.addProperty("group", this.group);
         }

         JsonArray jsonarray = new JsonArray();

         for(String s : this.pattern) {
            jsonarray.add(s);
         }

         pJson.add("pattern", jsonarray);
         JsonObject jsonobject = new JsonObject();

         for(Map.Entry<Character, Ingredient> entry : this.key.entrySet()) {
            jsonobject.add(String.valueOf(entry.getKey()), entry.getValue().toJson(false));
         }

         pJson.add("key", jsonobject);
         JsonObject jsonobject1 = new JsonObject();
         jsonobject1.addProperty("item", BuiltInRegistries.ITEM.getKey(this.result).toString());
         if (this.count > 1) {
            jsonobject1.addProperty("count", this.count);
         }

         pJson.add("result", jsonobject1);
         pJson.addProperty("show_notification", this.showNotification);
      }

      public RecipeSerializer<?> type() {
         return RecipeSerializer.SHAPED_RECIPE;
      }

      public ResourceLocation id() {
         return this.id;
      }

      public AdvancementHolder advancement() {
         return this.advancement;
      }
   }
}
