package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class LevitationTrigger extends SimpleCriterionTrigger<LevitationTrigger.TriggerInstance> {
   public LevitationTrigger.TriggerInstance createInstance(JsonObject pJson, Optional<ContextAwarePredicate> pPlayer, DeserializationContext pDeserializationContext) {
      Optional<DistancePredicate> optional = DistancePredicate.fromJson(pJson.get("distance"));
      MinMaxBounds.Ints minmaxbounds$ints = MinMaxBounds.Ints.fromJson(pJson.get("duration"));
      return new LevitationTrigger.TriggerInstance(pPlayer, optional, minmaxbounds$ints);
   }

   public void trigger(ServerPlayer pPlayer, Vec3 pStartPos, int pDuration) {
      this.trigger(pPlayer, (p_49124_) -> {
         return p_49124_.matches(pPlayer, pStartPos, pDuration);
      });
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final Optional<DistancePredicate> distance;
      private final MinMaxBounds.Ints duration;

      public TriggerInstance(Optional<ContextAwarePredicate> pPlayer, Optional<DistancePredicate> pDistance, MinMaxBounds.Ints pDuration) {
         super(pPlayer);
         this.distance = pDistance;
         this.duration = pDuration;
      }

      public static Criterion<LevitationTrigger.TriggerInstance> levitated(DistancePredicate pDistance) {
         return CriteriaTriggers.LEVITATION.createCriterion(new LevitationTrigger.TriggerInstance(Optional.empty(), Optional.of(pDistance), MinMaxBounds.Ints.ANY));
      }

      public boolean matches(ServerPlayer pPlayer, Vec3 pStartPos, int pDuration) {
         if (this.distance.isPresent() && !this.distance.get().matches(pStartPos.x, pStartPos.y, pStartPos.z, pPlayer.getX(), pPlayer.getY(), pPlayer.getZ())) {
            return false;
         } else {
            return this.duration.matches(pDuration);
         }
      }

      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         this.distance.ifPresent((p_300145_) -> {
            jsonobject.add("distance", p_300145_.serializeToJson());
         });
         jsonobject.add("duration", this.duration.serializeToJson());
         return jsonobject;
      }
   }
}