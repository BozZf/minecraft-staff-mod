package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Collection;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public class FishingRodHookedTrigger extends SimpleCriterionTrigger<FishingRodHookedTrigger.TriggerInstance> {
   public FishingRodHookedTrigger.TriggerInstance createInstance(JsonObject pJson, Optional<ContextAwarePredicate> pPlayer, DeserializationContext pDeserializationContext) {
      Optional<ItemPredicate> optional = ItemPredicate.fromJson(pJson.get("rod"));
      Optional<ContextAwarePredicate> optional1 = EntityPredicate.fromJson(pJson, "entity", pDeserializationContext);
      Optional<ItemPredicate> optional2 = ItemPredicate.fromJson(pJson.get("item"));
      return new FishingRodHookedTrigger.TriggerInstance(pPlayer, optional, optional1, optional2);
   }

   public void trigger(ServerPlayer pPlayer, ItemStack pRod, FishingHook pEntity, Collection<ItemStack> pStacks) {
      LootContext lootcontext = EntityPredicate.createContext(pPlayer, (Entity)(pEntity.getHookedIn() != null ? pEntity.getHookedIn() : pEntity));
      this.trigger(pPlayer, (p_40425_) -> {
         return p_40425_.matches(pRod, lootcontext, pStacks);
      });
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final Optional<ItemPredicate> rod;
      private final Optional<ContextAwarePredicate> entity;
      private final Optional<ItemPredicate> item;

      public TriggerInstance(Optional<ContextAwarePredicate> pPlayer, Optional<ItemPredicate> pRod, Optional<ContextAwarePredicate> pEntity, Optional<ItemPredicate> pItem) {
         super(pPlayer);
         this.rod = pRod;
         this.entity = pEntity;
         this.item = pItem;
      }

      public static Criterion<FishingRodHookedTrigger.TriggerInstance> fishedItem(Optional<ItemPredicate> pRod, Optional<EntityPredicate> pEntity, Optional<ItemPredicate> pItem) {
         return CriteriaTriggers.FISHING_ROD_HOOKED.createCriterion(new FishingRodHookedTrigger.TriggerInstance(Optional.empty(), pRod, EntityPredicate.wrap(pEntity), pItem));
      }

      public boolean matches(ItemStack pRod, LootContext pContext, Collection<ItemStack> pStacks) {
         if (this.rod.isPresent() && !this.rod.get().matches(pRod)) {
            return false;
         } else if (this.entity.isPresent() && !this.entity.get().matches(pContext)) {
            return false;
         } else {
            if (this.item.isPresent()) {
               boolean flag = false;
               Entity entity = pContext.getParamOrNull(LootContextParams.THIS_ENTITY);
               if (entity instanceof ItemEntity) {
                  ItemEntity itementity = (ItemEntity)entity;
                  if (this.item.get().matches(itementity.getItem())) {
                     flag = true;
                  }
               }

               for(ItemStack itemstack : pStacks) {
                  if (this.item.get().matches(itemstack)) {
                     flag = true;
                     break;
                  }
               }

               if (!flag) {
                  return false;
               }
            }

            return true;
         }
      }

      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         this.rod.ifPresent((p_299516_) -> {
            jsonobject.add("rod", p_299516_.serializeToJson());
         });
         this.entity.ifPresent((p_297815_) -> {
            jsonobject.add("entity", p_297815_.toJson());
         });
         this.item.ifPresent((p_297690_) -> {
            jsonobject.add("item", p_297690_.serializeToJson());
         });
         return jsonobject;
      }
   }
}