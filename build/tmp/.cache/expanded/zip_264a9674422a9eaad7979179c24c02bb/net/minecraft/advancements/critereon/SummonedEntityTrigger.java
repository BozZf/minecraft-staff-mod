package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;

public class SummonedEntityTrigger extends SimpleCriterionTrigger<SummonedEntityTrigger.TriggerInstance> {
   public SummonedEntityTrigger.TriggerInstance createInstance(JsonObject pJson, Optional<ContextAwarePredicate> pPlayer, DeserializationContext pDeserializationContext) {
      Optional<ContextAwarePredicate> optional = EntityPredicate.fromJson(pJson, "entity", pDeserializationContext);
      return new SummonedEntityTrigger.TriggerInstance(pPlayer, optional);
   }

   public void trigger(ServerPlayer pPlayer, Entity pEntity) {
      LootContext lootcontext = EntityPredicate.createContext(pPlayer, pEntity);
      this.trigger(pPlayer, (p_68265_) -> {
         return p_68265_.matches(lootcontext);
      });
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final Optional<ContextAwarePredicate> entity;

      public TriggerInstance(Optional<ContextAwarePredicate> pPlayer, Optional<ContextAwarePredicate> pEntity) {
         super(pPlayer);
         this.entity = pEntity;
      }

      public static Criterion<SummonedEntityTrigger.TriggerInstance> summonedEntity(EntityPredicate.Builder pEntity) {
         return CriteriaTriggers.SUMMONED_ENTITY.createCriterion(new SummonedEntityTrigger.TriggerInstance(Optional.empty(), Optional.of(EntityPredicate.wrap(pEntity))));
      }

      public boolean matches(LootContext pLootContext) {
         return this.entity.isEmpty() || this.entity.get().matches(pLootContext);
      }

      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         this.entity.ifPresent((p_298872_) -> {
            jsonobject.add("entity", p_298872_.toJson());
         });
         return jsonobject;
      }
   }
}