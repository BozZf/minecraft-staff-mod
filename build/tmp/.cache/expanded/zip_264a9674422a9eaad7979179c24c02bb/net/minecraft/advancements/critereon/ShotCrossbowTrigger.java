package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public class ShotCrossbowTrigger extends SimpleCriterionTrigger<ShotCrossbowTrigger.TriggerInstance> {
   public ShotCrossbowTrigger.TriggerInstance createInstance(JsonObject pJson, Optional<ContextAwarePredicate> pPlayer, DeserializationContext pDeserializationContext) {
      Optional<ItemPredicate> optional = ItemPredicate.fromJson(pJson.get("item"));
      return new ShotCrossbowTrigger.TriggerInstance(pPlayer, optional);
   }

   public void trigger(ServerPlayer pShooter, ItemStack pStack) {
      this.trigger(pShooter, (p_65467_) -> {
         return p_65467_.matches(pStack);
      });
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final Optional<ItemPredicate> item;

      public TriggerInstance(Optional<ContextAwarePredicate> pPlayer, Optional<ItemPredicate> pItem) {
         super(pPlayer);
         this.item = pItem;
      }

      public static Criterion<ShotCrossbowTrigger.TriggerInstance> shotCrossbow(Optional<ItemPredicate> pItem) {
         return CriteriaTriggers.SHOT_CROSSBOW.createCriterion(new ShotCrossbowTrigger.TriggerInstance(Optional.empty(), pItem));
      }

      public static Criterion<ShotCrossbowTrigger.TriggerInstance> shotCrossbow(ItemLike pItem) {
         return CriteriaTriggers.SHOT_CROSSBOW.createCriterion(new ShotCrossbowTrigger.TriggerInstance(Optional.empty(), Optional.of(ItemPredicate.Builder.item().of(pItem).build())));
      }

      public boolean matches(ItemStack pItem) {
         return this.item.isEmpty() || this.item.get().matches(pItem);
      }

      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         this.item.ifPresent((p_298022_) -> {
            jsonobject.add("item", p_298022_.serializeToJson());
         });
         return jsonobject;
      }
   }
}