package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;

public class RecipeCraftedTrigger extends SimpleCriterionTrigger<RecipeCraftedTrigger.TriggerInstance> {
   protected RecipeCraftedTrigger.TriggerInstance createInstance(JsonObject pJson, Optional<ContextAwarePredicate> pPlayer, DeserializationContext pDeserializationContext) {
      ResourceLocation resourcelocation = new ResourceLocation(GsonHelper.getAsString(pJson, "recipe_id"));
      List<ItemPredicate> list = ItemPredicate.fromJsonArray(pJson.get("ingredients"));
      return new RecipeCraftedTrigger.TriggerInstance(pPlayer, resourcelocation, list);
   }

   public void trigger(ServerPlayer pPlayer, ResourceLocation pRecipeId, List<ItemStack> pItems) {
      this.trigger(pPlayer, (p_282798_) -> {
         return p_282798_.matches(pRecipeId, pItems);
      });
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final ResourceLocation recipeId;
      private final List<ItemPredicate> predicates;

      public TriggerInstance(Optional<ContextAwarePredicate> pPlayer, ResourceLocation pRecipeId, List<ItemPredicate> pPredicates) {
         super(pPlayer);
         this.recipeId = pRecipeId;
         this.predicates = pPredicates;
      }

      public static Criterion<RecipeCraftedTrigger.TriggerInstance> craftedItem(ResourceLocation pRecipeId, List<ItemPredicate.Builder> pPredicates) {
         return CriteriaTriggers.RECIPE_CRAFTED.createCriterion(new RecipeCraftedTrigger.TriggerInstance(Optional.empty(), pRecipeId, pPredicates.stream().map(ItemPredicate.Builder::build).toList()));
      }

      public static Criterion<RecipeCraftedTrigger.TriggerInstance> craftedItem(ResourceLocation pRecipeId) {
         return CriteriaTriggers.RECIPE_CRAFTED.createCriterion(new RecipeCraftedTrigger.TriggerInstance(Optional.empty(), pRecipeId, List.of()));
      }

      boolean matches(ResourceLocation pRecipeId, List<ItemStack> pItems) {
         if (!pRecipeId.equals(this.recipeId)) {
            return false;
         } else {
            List<ItemStack> list = new ArrayList<>(pItems);

            for(ItemPredicate itempredicate : this.predicates) {
               boolean flag = false;
               Iterator<ItemStack> iterator = list.iterator();

               while(iterator.hasNext()) {
                  if (itempredicate.matches(iterator.next())) {
                     iterator.remove();
                     flag = true;
                     break;
                  }
               }

               if (!flag) {
                  return false;
               }
            }

            return true;
         }
      }

      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         jsonobject.addProperty("recipe_id", this.recipeId.toString());
         if (!this.predicates.isEmpty()) {
            jsonobject.add("ingredients", ItemPredicate.serializeToJsonArray(this.predicates));
         }

         return jsonobject;
      }
   }
}