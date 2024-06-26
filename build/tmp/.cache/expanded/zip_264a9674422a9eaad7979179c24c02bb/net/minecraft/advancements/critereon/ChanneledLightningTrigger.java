package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;

public class ChanneledLightningTrigger extends SimpleCriterionTrigger<ChanneledLightningTrigger.TriggerInstance> {
   public ChanneledLightningTrigger.TriggerInstance createInstance(JsonObject pJson, Optional<ContextAwarePredicate> pPlayer, DeserializationContext pDeserializationContext) {
      List<ContextAwarePredicate> list = EntityPredicate.fromJsonArray(pJson, "victims", pDeserializationContext);
      return new ChanneledLightningTrigger.TriggerInstance(pPlayer, list);
   }

   public void trigger(ServerPlayer pPlayer, Collection<? extends Entity> pEntityTriggered) {
      List<LootContext> list = pEntityTriggered.stream().map((p_21720_) -> {
         return EntityPredicate.createContext(pPlayer, p_21720_);
      }).collect(Collectors.toList());
      this.trigger(pPlayer, (p_21730_) -> {
         return p_21730_.matches(list);
      });
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final List<ContextAwarePredicate> victims;

      public TriggerInstance(Optional<ContextAwarePredicate> pPlayer, List<ContextAwarePredicate> pVictims) {
         super(pPlayer);
         this.victims = pVictims;
      }

      public static Criterion<ChanneledLightningTrigger.TriggerInstance> channeledLightning(EntityPredicate.Builder... pVictims) {
         return CriteriaTriggers.CHANNELED_LIGHTNING.createCriterion(new ChanneledLightningTrigger.TriggerInstance(Optional.empty(), EntityPredicate.wrap(pVictims)));
      }

      public boolean matches(Collection<? extends LootContext> pVictims) {
         for(ContextAwarePredicate contextawarepredicate : this.victims) {
            boolean flag = false;

            for(LootContext lootcontext : pVictims) {
               if (contextawarepredicate.matches(lootcontext)) {
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

      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         jsonobject.add("victims", ContextAwarePredicate.toJson(this.victims));
         return jsonobject;
      }
   }
}