package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.RecipeHolder;

public class RecipeUnlockedTrigger extends SimpleCriterionTrigger<RecipeUnlockedTrigger.TriggerInstance> {
   public RecipeUnlockedTrigger.TriggerInstance createInstance(JsonObject pJson, Optional<ContextAwarePredicate> pPlayer, DeserializationContext pDeserializationContext) {
      ResourceLocation resourcelocation = new ResourceLocation(GsonHelper.getAsString(pJson, "recipe"));
      return new RecipeUnlockedTrigger.TriggerInstance(pPlayer, resourcelocation);
   }

   public void trigger(ServerPlayer pPlayer, RecipeHolder<?> pRecipe) {
      this.trigger(pPlayer, (p_296143_) -> {
         return p_296143_.matches(pRecipe);
      });
   }

   public static Criterion<RecipeUnlockedTrigger.TriggerInstance> unlocked(ResourceLocation pRecipeId) {
      return CriteriaTriggers.RECIPE_UNLOCKED.createCriterion(new RecipeUnlockedTrigger.TriggerInstance(Optional.empty(), pRecipeId));
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final ResourceLocation recipe;

      public TriggerInstance(Optional<ContextAwarePredicate> pPlayer, ResourceLocation pRecipe) {
         super(pPlayer);
         this.recipe = pRecipe;
      }

      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         jsonobject.addProperty("recipe", this.recipe.toString());
         return jsonobject;
      }

      public boolean matches(RecipeHolder<?> pRecipe) {
         return this.recipe.equals(pRecipe.id());
      }
   }
}