package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class EnchantedItemTrigger extends SimpleCriterionTrigger<EnchantedItemTrigger.TriggerInstance> {
   public EnchantedItemTrigger.TriggerInstance createInstance(JsonObject pJson, Optional<ContextAwarePredicate> pPlayer, DeserializationContext pDeserializationContext) {
      Optional<ItemPredicate> optional = ItemPredicate.fromJson(pJson.get("item"));
      MinMaxBounds.Ints minmaxbounds$ints = MinMaxBounds.Ints.fromJson(pJson.get("levels"));
      return new EnchantedItemTrigger.TriggerInstance(pPlayer, optional, minmaxbounds$ints);
   }

   public void trigger(ServerPlayer pPlayer, ItemStack pItem, int pLevelsSpent) {
      this.trigger(pPlayer, (p_27675_) -> {
         return p_27675_.matches(pItem, pLevelsSpent);
      });
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final Optional<ItemPredicate> item;
      private final MinMaxBounds.Ints levels;

      public TriggerInstance(Optional<ContextAwarePredicate> pPlayer, Optional<ItemPredicate> pItem, MinMaxBounds.Ints pLevels) {
         super(pPlayer);
         this.item = pItem;
         this.levels = pLevels;
      }

      public static Criterion<EnchantedItemTrigger.TriggerInstance> enchantedItem() {
         return CriteriaTriggers.ENCHANTED_ITEM.createCriterion(new EnchantedItemTrigger.TriggerInstance(Optional.empty(), Optional.empty(), MinMaxBounds.Ints.ANY));
      }

      public boolean matches(ItemStack pItem, int pLevels) {
         if (this.item.isPresent() && !this.item.get().matches(pItem)) {
            return false;
         } else {
            return this.levels.matches(pLevels);
         }
      }

      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         this.item.ifPresent((p_297307_) -> {
            jsonobject.add("item", p_297307_.serializeToJson());
         });
         jsonobject.add("levels", this.levels.serializeToJson());
         return jsonobject;
      }
   }
}