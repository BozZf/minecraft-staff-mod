package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class UsingItemTrigger extends SimpleCriterionTrigger<UsingItemTrigger.TriggerInstance> {
   public UsingItemTrigger.TriggerInstance createInstance(JsonObject pJson, Optional<ContextAwarePredicate> pPlayer, DeserializationContext pDeserializationContext) {
      Optional<ItemPredicate> optional = ItemPredicate.fromJson(pJson.get("item"));
      return new UsingItemTrigger.TriggerInstance(pPlayer, optional);
   }

   public void trigger(ServerPlayer pPlayer, ItemStack pItem) {
      this.trigger(pPlayer, (p_163870_) -> {
         return p_163870_.matches(pItem);
      });
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final Optional<ItemPredicate> item;

      public TriggerInstance(Optional<ContextAwarePredicate> pPlayer, Optional<ItemPredicate> pItem) {
         super(pPlayer);
         this.item = pItem;
      }

      public static Criterion<UsingItemTrigger.TriggerInstance> lookingAt(EntityPredicate.Builder pPlayer, ItemPredicate.Builder pItem) {
         return CriteriaTriggers.USING_ITEM.createCriterion(new UsingItemTrigger.TriggerInstance(Optional.of(EntityPredicate.wrap(pPlayer)), Optional.of(pItem.build())));
      }

      public boolean matches(ItemStack pItem) {
         return !this.item.isPresent() || this.item.get().matches(pItem);
      }

      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         this.item.ifPresent((p_300679_) -> {
            jsonobject.add("item", p_300679_.serializeToJson());
         });
         return jsonobject;
      }
   }
}