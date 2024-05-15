package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.storage.loot.LootContext;

public class BredAnimalsTrigger extends SimpleCriterionTrigger<BredAnimalsTrigger.TriggerInstance> {
   public BredAnimalsTrigger.TriggerInstance createInstance(JsonObject pJson, Optional<ContextAwarePredicate> pPlayer, DeserializationContext pDeserializationContext) {
      Optional<ContextAwarePredicate> optional = EntityPredicate.fromJson(pJson, "parent", pDeserializationContext);
      Optional<ContextAwarePredicate> optional1 = EntityPredicate.fromJson(pJson, "partner", pDeserializationContext);
      Optional<ContextAwarePredicate> optional2 = EntityPredicate.fromJson(pJson, "child", pDeserializationContext);
      return new BredAnimalsTrigger.TriggerInstance(pPlayer, optional, optional1, optional2);
   }

   public void trigger(ServerPlayer pPlayer, Animal pParent, Animal pPartner, @Nullable AgeableMob pChild) {
      LootContext lootcontext = EntityPredicate.createContext(pPlayer, pParent);
      LootContext lootcontext1 = EntityPredicate.createContext(pPlayer, pPartner);
      LootContext lootcontext2 = pChild != null ? EntityPredicate.createContext(pPlayer, pChild) : null;
      this.trigger(pPlayer, (p_18653_) -> {
         return p_18653_.matches(lootcontext, lootcontext1, lootcontext2);
      });
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final Optional<ContextAwarePredicate> parent;
      private final Optional<ContextAwarePredicate> partner;
      private final Optional<ContextAwarePredicate> child;

      public TriggerInstance(Optional<ContextAwarePredicate> pPlayer, Optional<ContextAwarePredicate> pParent, Optional<ContextAwarePredicate> pPartner, Optional<ContextAwarePredicate> pChild) {
         super(pPlayer);
         this.parent = pParent;
         this.partner = pPartner;
         this.child = pChild;
      }

      public static Criterion<BredAnimalsTrigger.TriggerInstance> bredAnimals() {
         return CriteriaTriggers.BRED_ANIMALS.createCriterion(new BredAnimalsTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
      }

      public static Criterion<BredAnimalsTrigger.TriggerInstance> bredAnimals(EntityPredicate.Builder pChild) {
         return CriteriaTriggers.BRED_ANIMALS.createCriterion(new BredAnimalsTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(EntityPredicate.wrap(pChild))));
      }

      public static Criterion<BredAnimalsTrigger.TriggerInstance> bredAnimals(Optional<EntityPredicate> pParent, Optional<EntityPredicate> pPartner, Optional<EntityPredicate> pChild) {
         return CriteriaTriggers.BRED_ANIMALS.createCriterion(new BredAnimalsTrigger.TriggerInstance(Optional.empty(), EntityPredicate.wrap(pParent), EntityPredicate.wrap(pPartner), EntityPredicate.wrap(pChild)));
      }

      public boolean matches(LootContext pParentContext, LootContext pPartnerContext, @Nullable LootContext pChildContext) {
         if (!this.child.isPresent() || pChildContext != null && this.child.get().matches(pChildContext)) {
            return matches(this.parent, pParentContext) && matches(this.partner, pPartnerContext) || matches(this.parent, pPartnerContext) && matches(this.partner, pParentContext);
         } else {
            return false;
         }
      }

      private static boolean matches(Optional<ContextAwarePredicate> pPredicate, LootContext pContext) {
         return pPredicate.isEmpty() || pPredicate.get().matches(pContext);
      }

      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         this.parent.ifPresent((p_297504_) -> {
            jsonobject.add("parent", p_297504_.toJson());
         });
         this.partner.ifPresent((p_301179_) -> {
            jsonobject.add("partner", p_301179_.toJson());
         });
         this.child.ifPresent((p_298465_) -> {
            jsonobject.add("child", p_298465_.toJson());
         });
         return jsonobject;
      }
   }
}