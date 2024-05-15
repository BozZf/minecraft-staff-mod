package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;

public class TradeTrigger extends SimpleCriterionTrigger<TradeTrigger.TriggerInstance> {
   public TradeTrigger.TriggerInstance createInstance(JsonObject pJson, Optional<ContextAwarePredicate> pPlayer, DeserializationContext pDeserializationContext) {
      Optional<ContextAwarePredicate> optional = EntityPredicate.fromJson(pJson, "villager", pDeserializationContext);
      Optional<ItemPredicate> optional1 = ItemPredicate.fromJson(pJson.get("item"));
      return new TradeTrigger.TriggerInstance(pPlayer, optional, optional1);
   }

   public void trigger(ServerPlayer pPlayer, AbstractVillager pVillager, ItemStack pStack) {
      LootContext lootcontext = EntityPredicate.createContext(pPlayer, pVillager);
      this.trigger(pPlayer, (p_70970_) -> {
         return p_70970_.matches(lootcontext, pStack);
      });
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final Optional<ContextAwarePredicate> villager;
      private final Optional<ItemPredicate> item;

      public TriggerInstance(Optional<ContextAwarePredicate> pPlayer, Optional<ContextAwarePredicate> pVillager, Optional<ItemPredicate> pItem) {
         super(pPlayer);
         this.villager = pVillager;
         this.item = pItem;
      }

      public static Criterion<TradeTrigger.TriggerInstance> tradedWithVillager() {
         return CriteriaTriggers.TRADE.createCriterion(new TradeTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.empty()));
      }

      public static Criterion<TradeTrigger.TriggerInstance> tradedWithVillager(EntityPredicate.Builder pVillager) {
         return CriteriaTriggers.TRADE.createCriterion(new TradeTrigger.TriggerInstance(Optional.of(EntityPredicate.wrap(pVillager)), Optional.empty(), Optional.empty()));
      }

      public boolean matches(LootContext pContext, ItemStack pStack) {
         if (this.villager.isPresent() && !this.villager.get().matches(pContext)) {
            return false;
         } else {
            return !this.item.isPresent() || this.item.get().matches(pStack);
         }
      }

      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         this.item.ifPresent((p_301255_) -> {
            jsonobject.add("item", p_301255_.serializeToJson());
         });
         this.villager.ifPresent((p_299256_) -> {
            jsonobject.add("villager", p_299256_.toJson());
         });
         return jsonobject;
      }
   }
}