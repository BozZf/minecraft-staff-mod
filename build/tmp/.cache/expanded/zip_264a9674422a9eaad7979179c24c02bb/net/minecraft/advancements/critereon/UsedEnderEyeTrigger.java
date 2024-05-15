package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

public class UsedEnderEyeTrigger extends SimpleCriterionTrigger<UsedEnderEyeTrigger.TriggerInstance> {
   public UsedEnderEyeTrigger.TriggerInstance createInstance(JsonObject pJson, Optional<ContextAwarePredicate> pPlayer, DeserializationContext pDeserializationContext) {
      MinMaxBounds.Doubles minmaxbounds$doubles = MinMaxBounds.Doubles.fromJson(pJson.get("distance"));
      return new UsedEnderEyeTrigger.TriggerInstance(pPlayer, minmaxbounds$doubles);
   }

   public void trigger(ServerPlayer pPlayer, BlockPos pPos) {
      double d0 = pPlayer.getX() - (double)pPos.getX();
      double d1 = pPlayer.getZ() - (double)pPos.getZ();
      double d2 = d0 * d0 + d1 * d1;
      this.trigger(pPlayer, (p_73934_) -> {
         return p_73934_.matches(d2);
      });
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final MinMaxBounds.Doubles level;

      public TriggerInstance(Optional<ContextAwarePredicate> pPlayer, MinMaxBounds.Doubles pLevel) {
         super(pPlayer);
         this.level = pLevel;
      }

      public boolean matches(double pDistanceSq) {
         return this.level.matchesSqr(pDistanceSq);
      }
   }
}