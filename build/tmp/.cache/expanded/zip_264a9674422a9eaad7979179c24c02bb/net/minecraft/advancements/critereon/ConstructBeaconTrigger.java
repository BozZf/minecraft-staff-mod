package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;

public class ConstructBeaconTrigger extends SimpleCriterionTrigger<ConstructBeaconTrigger.TriggerInstance> {
   public ConstructBeaconTrigger.TriggerInstance createInstance(JsonObject pJson, Optional<ContextAwarePredicate> pPlayer, DeserializationContext pDeserializationContext) {
      MinMaxBounds.Ints minmaxbounds$ints = MinMaxBounds.Ints.fromJson(pJson.get("level"));
      return new ConstructBeaconTrigger.TriggerInstance(pPlayer, minmaxbounds$ints);
   }

   public void trigger(ServerPlayer pPlayer, int pLevel) {
      this.trigger(pPlayer, (p_148028_) -> {
         return p_148028_.matches(pLevel);
      });
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final MinMaxBounds.Ints level;

      public TriggerInstance(Optional<ContextAwarePredicate> pPlayer, MinMaxBounds.Ints pLevel) {
         super(pPlayer);
         this.level = pLevel;
      }

      public static Criterion<ConstructBeaconTrigger.TriggerInstance> constructedBeacon() {
         return CriteriaTriggers.CONSTRUCT_BEACON.createCriterion(new ConstructBeaconTrigger.TriggerInstance(Optional.empty(), MinMaxBounds.Ints.ANY));
      }

      public static Criterion<ConstructBeaconTrigger.TriggerInstance> constructedBeacon(MinMaxBounds.Ints pLevel) {
         return CriteriaTriggers.CONSTRUCT_BEACON.createCriterion(new ConstructBeaconTrigger.TriggerInstance(Optional.empty(), pLevel));
      }

      public boolean matches(int pLevel) {
         return this.level.matches(pLevel);
      }

      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         jsonobject.add("level", this.level.serializeToJson());
         return jsonobject;
      }
   }
}