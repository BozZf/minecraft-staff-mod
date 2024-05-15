package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public class ConsumeItemTrigger extends SimpleCriterionTrigger<ConsumeItemTrigger.TriggerInstance> {
   public ConsumeItemTrigger.TriggerInstance createInstance(JsonObject pJson, Optional<ContextAwarePredicate> pPlayer, DeserializationContext pDeserializationContext) {
      return new ConsumeItemTrigger.TriggerInstance(pPlayer, ItemPredicate.fromJson(pJson.get("item")));
   }

   public void trigger(ServerPlayer pPlayer, ItemStack pItem) {
      this.trigger(pPlayer, (p_23687_) -> {
         return p_23687_.matches(pItem);
      });
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final Optional<ItemPredicate> item;

      public TriggerInstance(Optional<ContextAwarePredicate> pPlayer, Optional<ItemPredicate> pItem) {
         super(pPlayer);
         this.item = pItem;
      }

      public static Criterion<ConsumeItemTrigger.TriggerInstance> usedItem() {
         return CriteriaTriggers.CONSUME_ITEM.createCriterion(new ConsumeItemTrigger.TriggerInstance(Optional.empty(), Optional.empty()));
      }

      public static Criterion<ConsumeItemTrigger.TriggerInstance> usedItem(ItemLike pItem) {
         return usedItem(ItemPredicate.Builder.item().of(pItem.asItem()));
      }

      public static Criterion<ConsumeItemTrigger.TriggerInstance> usedItem(ItemPredicate.Builder pItem) {
         return CriteriaTriggers.CONSUME_ITEM.createCriterion(new ConsumeItemTrigger.TriggerInstance(Optional.empty(), Optional.of(pItem.build())));
      }

      public boolean matches(ItemStack pItem) {
         return this.item.isEmpty() || this.item.get().matches(pItem);
      }

      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         this.item.ifPresent((p_299494_) -> {
            jsonobject.add("item", p_299494_.serializeToJson());
         });
         return jsonobject;
      }
   }
}