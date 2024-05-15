package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;

public class PickedUpItemTrigger extends SimpleCriterionTrigger<PickedUpItemTrigger.TriggerInstance> {
   protected PickedUpItemTrigger.TriggerInstance createInstance(JsonObject pJson, Optional<ContextAwarePredicate> pPlayer, DeserializationContext pDeserializationContext) {
      Optional<ItemPredicate> optional = ItemPredicate.fromJson(pJson.get("item"));
      Optional<ContextAwarePredicate> optional1 = EntityPredicate.fromJson(pJson, "entity", pDeserializationContext);
      return new PickedUpItemTrigger.TriggerInstance(pPlayer, optional, optional1);
   }

   public void trigger(ServerPlayer pPlayer, ItemStack pStack, @Nullable Entity pEntity) {
      LootContext lootcontext = EntityPredicate.createContext(pPlayer, pEntity);
      this.trigger(pPlayer, (p_221306_) -> {
         return p_221306_.matches(pPlayer, pStack, lootcontext);
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

      public static Criterion<PickedUpItemTrigger.TriggerInstance> thrownItemPickedUpByEntity(ContextAwarePredicate pPlayer, Optional<ItemPredicate> pItem, Optional<ContextAwarePredicate> pEntity) {
         return CriteriaTriggers.THROWN_ITEM_PICKED_UP_BY_ENTITY.createCriterion(new PickedUpItemTrigger.TriggerInstance(Optional.of(pPlayer), pItem, pEntity));
      }

      public static Criterion<PickedUpItemTrigger.TriggerInstance> thrownItemPickedUpByPlayer(Optional<ContextAwarePredicate> pPlayer, Optional<ItemPredicate> pItem, Optional<ContextAwarePredicate> pEntity) {
         return CriteriaTriggers.THROWN_ITEM_PICKED_UP_BY_PLAYER.createCriterion(new PickedUpItemTrigger.TriggerInstance(pPlayer, pItem, pEntity));
      }

      public boolean matches(ServerPlayer pPlayer, ItemStack pStack, LootContext pContext) {
         if (this.item.isPresent() && !this.item.get().matches(pStack)) {
            return false;
         } else {
            return !this.entity.isPresent() || this.entity.get().matches(pContext);
         }
      }

      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         this.item.ifPresent((p_300141_) -> {
            jsonobject.add("item", p_300141_.serializeToJson());
         });
         this.entity.ifPresent((p_299817_) -> {
            jsonobject.add("entity", p_299817_.toJson());
         });
         return jsonobject;
      }
   }
}