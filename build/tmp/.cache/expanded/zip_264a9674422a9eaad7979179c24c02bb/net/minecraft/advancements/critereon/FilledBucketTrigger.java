package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class FilledBucketTrigger extends SimpleCriterionTrigger<FilledBucketTrigger.TriggerInstance> {
   public FilledBucketTrigger.TriggerInstance createInstance(JsonObject pJson, Optional<ContextAwarePredicate> pPlayer, DeserializationContext pDeserializationContext) {
      Optional<ItemPredicate> optional = ItemPredicate.fromJson(pJson.get("item"));
      return new FilledBucketTrigger.TriggerInstance(pPlayer, optional);
   }

   public void trigger(ServerPlayer pPlayer, ItemStack pStack) {
      this.trigger(pPlayer, (p_38777_) -> {
         return p_38777_.matches(pStack);
      });
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final Optional<ItemPredicate> item;

      public TriggerInstance(Optional<ContextAwarePredicate> pPlayer, Optional<ItemPredicate> pItem) {
         super(pPlayer);
         this.item = pItem;
      }

      public static Criterion<FilledBucketTrigger.TriggerInstance> filledBucket(ItemPredicate.Builder pItem) {
         return CriteriaTriggers.FILLED_BUCKET.createCriterion(new FilledBucketTrigger.TriggerInstance(Optional.empty(), Optional.of(pItem.build())));
      }

      public boolean matches(ItemStack pStack) {
         return !this.item.isPresent() || this.item.get().matches(pStack);
      }

      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         this.item.ifPresent((p_300984_) -> {
            jsonobject.add("item", p_300984_.serializeToJson());
         });
         return jsonobject;
      }
   }
}