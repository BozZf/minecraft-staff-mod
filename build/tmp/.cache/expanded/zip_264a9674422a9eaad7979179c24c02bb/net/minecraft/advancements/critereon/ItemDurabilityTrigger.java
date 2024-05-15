package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class ItemDurabilityTrigger extends SimpleCriterionTrigger<ItemDurabilityTrigger.TriggerInstance> {
   public ItemDurabilityTrigger.TriggerInstance createInstance(JsonObject pJson, Optional<ContextAwarePredicate> pPlayer, DeserializationContext pDeserializationContext) {
      Optional<ItemPredicate> optional = ItemPredicate.fromJson(pJson.get("item"));
      MinMaxBounds.Ints minmaxbounds$ints = MinMaxBounds.Ints.fromJson(pJson.get("durability"));
      MinMaxBounds.Ints minmaxbounds$ints1 = MinMaxBounds.Ints.fromJson(pJson.get("delta"));
      return new ItemDurabilityTrigger.TriggerInstance(pPlayer, optional, minmaxbounds$ints, minmaxbounds$ints1);
   }

   public void trigger(ServerPlayer pPlayer, ItemStack pItem, int pNewDurability) {
      this.trigger(pPlayer, (p_43676_) -> {
         return p_43676_.matches(pItem, pNewDurability);
      });
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final Optional<ItemPredicate> item;
      private final MinMaxBounds.Ints durability;
      private final MinMaxBounds.Ints delta;

      public TriggerInstance(Optional<ContextAwarePredicate> pPlayer, Optional<ItemPredicate> pItem, MinMaxBounds.Ints pDurability, MinMaxBounds.Ints pDelta) {
         super(pPlayer);
         this.item = pItem;
         this.durability = pDurability;
         this.delta = pDelta;
      }

      public static Criterion<ItemDurabilityTrigger.TriggerInstance> changedDurability(Optional<ItemPredicate> pItem, MinMaxBounds.Ints pDurability) {
         return changedDurability(Optional.empty(), pItem, pDurability);
      }

      public static Criterion<ItemDurabilityTrigger.TriggerInstance> changedDurability(Optional<ContextAwarePredicate> pPlayer, Optional<ItemPredicate> pItem, MinMaxBounds.Ints pDurability) {
         return CriteriaTriggers.ITEM_DURABILITY_CHANGED.createCriterion(new ItemDurabilityTrigger.TriggerInstance(pPlayer, pItem, pDurability, MinMaxBounds.Ints.ANY));
      }

      public boolean matches(ItemStack pItem, int pDurability) {
         if (this.item.isPresent() && !this.item.get().matches(pItem)) {
            return false;
         } else if (!this.durability.matches(pItem.getMaxDamage() - pDurability)) {
            return false;
         } else {
            return this.delta.matches(pItem.getDamageValue() - pDurability);
         }
      }

      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         this.item.ifPresent((p_297308_) -> {
            jsonobject.add("item", p_297308_.serializeToJson());
         });
         jsonobject.add("durability", this.durability.serializeToJson());
         jsonobject.add("delta", this.delta.serializeToJson());
         return jsonobject;
      }
   }
}