package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public class UsedTotemTrigger extends SimpleCriterionTrigger<UsedTotemTrigger.TriggerInstance> {
   public UsedTotemTrigger.TriggerInstance createInstance(JsonObject pJson, Optional<ContextAwarePredicate> pPlayer, DeserializationContext pDeserializationContext) {
      Optional<ItemPredicate> optional = ItemPredicate.fromJson(pJson.get("item"));
      return new UsedTotemTrigger.TriggerInstance(pPlayer, optional);
   }

   public void trigger(ServerPlayer pPlayer, ItemStack pItem) {
      this.trigger(pPlayer, (p_74436_) -> {
         return p_74436_.matches(pItem);
      });
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final Optional<ItemPredicate> item;

      public TriggerInstance(Optional<ContextAwarePredicate> pPlayer, Optional<ItemPredicate> pItem) {
         super(pPlayer);
         this.item = pItem;
      }

      public static Criterion<UsedTotemTrigger.TriggerInstance> usedTotem(ItemPredicate pItem) {
         return CriteriaTriggers.USED_TOTEM.createCriterion(new UsedTotemTrigger.TriggerInstance(Optional.empty(), Optional.of(pItem)));
      }

      public static Criterion<UsedTotemTrigger.TriggerInstance> usedTotem(ItemLike pItem) {
         return CriteriaTriggers.USED_TOTEM.createCriterion(new UsedTotemTrigger.TriggerInstance(Optional.empty(), Optional.of(ItemPredicate.Builder.item().of(pItem).build())));
      }

      public boolean matches(ItemStack pItem) {
         return this.item.isEmpty() || this.item.get().matches(pItem);
      }

      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         this.item.ifPresent((p_297715_) -> {
            jsonobject.add("item", p_297715_.serializeToJson());
         });
         return jsonobject;
      }
   }
}