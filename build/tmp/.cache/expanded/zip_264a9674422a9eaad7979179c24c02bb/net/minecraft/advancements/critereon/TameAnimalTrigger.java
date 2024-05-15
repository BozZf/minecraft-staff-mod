package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.storage.loot.LootContext;

public class TameAnimalTrigger extends SimpleCriterionTrigger<TameAnimalTrigger.TriggerInstance> {
   public TameAnimalTrigger.TriggerInstance createInstance(JsonObject pJson, Optional<ContextAwarePredicate> pPlayer, DeserializationContext pDeserializationContext) {
      Optional<ContextAwarePredicate> optional = EntityPredicate.fromJson(pJson, "entity", pDeserializationContext);
      return new TameAnimalTrigger.TriggerInstance(pPlayer, optional);
   }

   public void trigger(ServerPlayer pPlayer, Animal pEntity) {
      LootContext lootcontext = EntityPredicate.createContext(pPlayer, pEntity);
      this.trigger(pPlayer, (p_68838_) -> {
         return p_68838_.matches(lootcontext);
      });
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final Optional<ContextAwarePredicate> entity;

      public TriggerInstance(Optional<ContextAwarePredicate> pPlayer, Optional<ContextAwarePredicate> pEntity) {
         super(pPlayer);
         this.entity = pEntity;
      }

      public static Criterion<TameAnimalTrigger.TriggerInstance> tamedAnimal() {
         return CriteriaTriggers.TAME_ANIMAL.createCriterion(new TameAnimalTrigger.TriggerInstance(Optional.empty(), Optional.empty()));
      }

      public static Criterion<TameAnimalTrigger.TriggerInstance> tamedAnimal(EntityPredicate.Builder pEntity) {
         return CriteriaTriggers.TAME_ANIMAL.createCriterion(new TameAnimalTrigger.TriggerInstance(Optional.empty(), Optional.of(EntityPredicate.wrap(pEntity))));
      }

      public boolean matches(LootContext pLootContext) {
         return this.entity.isEmpty() || this.entity.get().matches(pLootContext);
      }

      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         this.entity.ifPresent((p_298543_) -> {
            jsonobject.add("entity", p_298543_.toJson());
         });
         return jsonobject;
      }
   }
}