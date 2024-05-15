package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.Vec3;

public class TargetBlockTrigger extends SimpleCriterionTrigger<TargetBlockTrigger.TriggerInstance> {
   public TargetBlockTrigger.TriggerInstance createInstance(JsonObject pJson, Optional<ContextAwarePredicate> pPlayer, DeserializationContext pDeserializationContext) {
      MinMaxBounds.Ints minmaxbounds$ints = MinMaxBounds.Ints.fromJson(pJson.get("signal_strength"));
      Optional<ContextAwarePredicate> optional = EntityPredicate.fromJson(pJson, "projectile", pDeserializationContext);
      return new TargetBlockTrigger.TriggerInstance(pPlayer, minmaxbounds$ints, optional);
   }

   public void trigger(ServerPlayer pPlayer, Entity pProjectile, Vec3 pVector, int pSignalStrength) {
      LootContext lootcontext = EntityPredicate.createContext(pPlayer, pProjectile);
      this.trigger(pPlayer, (p_70224_) -> {
         return p_70224_.matches(lootcontext, pVector, pSignalStrength);
      });
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final MinMaxBounds.Ints signalStrength;
      private final Optional<ContextAwarePredicate> projectile;

      public TriggerInstance(Optional<ContextAwarePredicate> pPlayer, MinMaxBounds.Ints pSignalStrength, Optional<ContextAwarePredicate> pProjectile) {
         super(pPlayer);
         this.signalStrength = pSignalStrength;
         this.projectile = pProjectile;
      }

      public static Criterion<TargetBlockTrigger.TriggerInstance> targetHit(MinMaxBounds.Ints pSignalStrength, Optional<ContextAwarePredicate> pProjectile) {
         return CriteriaTriggers.TARGET_BLOCK_HIT.createCriterion(new TargetBlockTrigger.TriggerInstance(Optional.empty(), pSignalStrength, pProjectile));
      }

      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         jsonobject.add("signal_strength", this.signalStrength.serializeToJson());
         this.projectile.ifPresent((p_300972_) -> {
            jsonobject.add("projectile", p_300972_.toJson());
         });
         return jsonobject;
      }

      public boolean matches(LootContext pContext, Vec3 pVector, int pSignalStrength) {
         if (!this.signalStrength.matches(pSignalStrength)) {
            return false;
         } else {
            return !this.projectile.isPresent() || this.projectile.get().matches(pContext);
         }
      }
   }
}