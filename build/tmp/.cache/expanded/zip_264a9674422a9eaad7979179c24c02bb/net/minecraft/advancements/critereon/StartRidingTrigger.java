package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;

public class StartRidingTrigger extends SimpleCriterionTrigger<StartRidingTrigger.TriggerInstance> {
   public StartRidingTrigger.TriggerInstance createInstance(JsonObject pJson, Optional<ContextAwarePredicate> pPlayer, DeserializationContext pDeserializationContext) {
      return new StartRidingTrigger.TriggerInstance(pPlayer);
   }

   public void trigger(ServerPlayer pPlayer) {
      this.trigger(pPlayer, (p_160394_) -> {
         return true;
      });
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      public TriggerInstance(Optional<ContextAwarePredicate> pPlayer) {
         super(pPlayer);
      }

      public static Criterion<StartRidingTrigger.TriggerInstance> playerStartsRiding(EntityPredicate.Builder pPlayer) {
         return CriteriaTriggers.START_RIDING_TRIGGER.createCriterion(new StartRidingTrigger.TriggerInstance(Optional.of(EntityPredicate.wrap(pPlayer))));
      }
   }
}