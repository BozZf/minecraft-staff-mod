package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class DistanceTrigger extends SimpleCriterionTrigger<DistanceTrigger.TriggerInstance> {
   public DistanceTrigger.TriggerInstance createInstance(JsonObject pJson, Optional<ContextAwarePredicate> pPlayer, DeserializationContext pDeserializationContext) {
      Optional<LocationPredicate> optional = LocationPredicate.fromJson(pJson.get("start_position"));
      Optional<DistancePredicate> optional1 = DistancePredicate.fromJson(pJson.get("distance"));
      return new DistanceTrigger.TriggerInstance(pPlayer, optional, optional1);
   }

   public void trigger(ServerPlayer pPlayer, Vec3 pPosition) {
      Vec3 vec3 = pPlayer.position();
      this.trigger(pPlayer, (p_284572_) -> {
         return p_284572_.matches(pPlayer.serverLevel(), pPosition, vec3);
      });
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final Optional<LocationPredicate> startPosition;
      private final Optional<DistancePredicate> distance;

      public TriggerInstance(Optional<ContextAwarePredicate> pPlayer, Optional<LocationPredicate> pStartPosition, Optional<DistancePredicate> pDistance) {
         super(pPlayer);
         this.startPosition = pStartPosition;
         this.distance = pDistance;
      }

      public static Criterion<DistanceTrigger.TriggerInstance> fallFromHeight(EntityPredicate.Builder pPlayer, DistancePredicate pDistance, LocationPredicate.Builder pStartPosition) {
         return CriteriaTriggers.FALL_FROM_HEIGHT.createCriterion(new DistanceTrigger.TriggerInstance(Optional.of(EntityPredicate.wrap(pPlayer)), Optional.of(pStartPosition.build()), Optional.of(pDistance)));
      }

      public static Criterion<DistanceTrigger.TriggerInstance> rideEntityInLava(EntityPredicate.Builder pPlayer, DistancePredicate pDistance) {
         return CriteriaTriggers.RIDE_ENTITY_IN_LAVA_TRIGGER.createCriterion(new DistanceTrigger.TriggerInstance(Optional.of(EntityPredicate.wrap(pPlayer)), Optional.empty(), Optional.of(pDistance)));
      }

      public static Criterion<DistanceTrigger.TriggerInstance> travelledThroughNether(DistancePredicate pDistance) {
         return CriteriaTriggers.NETHER_TRAVEL.createCriterion(new DistanceTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.of(pDistance)));
      }

      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         this.startPosition.ifPresent((p_300367_) -> {
            jsonobject.add("start_position", p_300367_.serializeToJson());
         });
         this.distance.ifPresent((p_298273_) -> {
            jsonobject.add("distance", p_298273_.serializeToJson());
         });
         return jsonobject;
      }

      public boolean matches(ServerLevel pLevel, Vec3 pStartPosition, Vec3 pCurrentPosition) {
         if (this.startPosition.isPresent() && !this.startPosition.get().matches(pLevel, pStartPosition.x, pStartPosition.y, pStartPosition.z)) {
            return false;
         } else {
            return !this.distance.isPresent() || this.distance.get().matches(pStartPosition.x, pStartPosition.y, pStartPosition.z, pCurrentPosition.x, pCurrentPosition.y, pCurrentPosition.z);
         }
      }
   }
}