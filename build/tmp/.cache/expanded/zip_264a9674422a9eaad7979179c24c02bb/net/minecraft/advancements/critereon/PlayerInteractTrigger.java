package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;

public class PlayerInteractTrigger extends SimpleCriterionTrigger<PlayerInteractTrigger.TriggerInstance> {
   protected PlayerInteractTrigger.TriggerInstance createInstance(JsonObject pJson, Optional<ContextAwarePredicate> pPlayer, DeserializationContext pDeserializationContext) {
      Optional<ItemPredicate> optional = ItemPredicate.fromJson(pJson.get("item"));
      Optional<ContextAwarePredicate> optional1 = EntityPredicate.fromJson(pJson, "entity", pDeserializationContext);
      return new PlayerInteractTrigger.TriggerInstance(pPlayer, optional, optional1);
   }

   public void trigger(ServerPlayer pPlayer, ItemStack pItem, Entity pEntity) {
      LootContext lootcontext = EntityPredicate.createContext(pPlayer, pEntity);
      this.trigger(pPlayer, (p_61501_) -> {
         return p_61501_.matches(pItem, lootcontext);
      });
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final Optional<ItemPredicate> item;
      private final Optional<ContextAwarePredicate> entity;

      public TriggerInstance(Optional<ContextAwarePredicate> pPlayer, Optional<ItemPredicate> pItem, Optional<ContextAwarePredicate> pEntity) {
         super(pPlayer);
         this.item = pItem;
         this.entity = pEntity;
      }

      public static Criterion<PlayerInteractTrigger.TriggerInstance> itemUsedOnEntity(Optional<ContextAwarePredicate> pPlayer, ItemPredicate.Builder pItem, Optional<ContextAwarePredicate> pEntity) {
         return CriteriaTriggers.PLAYER_INTERACTED_WITH_ENTITY.createCriterion(new PlayerInteractTrigger.TriggerInstance(pPlayer, Optional.of(pItem.build()), pEntity));
      }

      public static Criterion<PlayerInteractTrigger.TriggerInstance> itemUsedOnEntity(ItemPredicate.Builder pItem, Optional<ContextAwarePredicate> pEntity) {
         return itemUsedOnEntity(Optional.empty(), pItem, pEntity);
      }

      public boolean matches(ItemStack pItem, LootContext pLootContext) {
         if (this.item.isPresent() && !this.item.get().matches(pItem)) {
            return false;
         } else {
            return this.entity.isEmpty() || this.entity.get().matches(pLootContext);
         }
      }

      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         this.item.ifPresent((p_298601_) -> {
            jsonobject.add("item", p_298601_.serializeToJson());
         });
         this.entity.ifPresent((p_299865_) -> {
            jsonobject.add("entity", p_299865_.toJson());
         });
         return jsonobject;
      }
   }
}