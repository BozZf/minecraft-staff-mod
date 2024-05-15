package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.storage.loot.LootContext;

public class LightningStrikeTrigger extends SimpleCriterionTrigger<LightningStrikeTrigger.TriggerInstance> {
   public LightningStrikeTrigger.TriggerInstance createInstance(JsonObject pJson, Optional<ContextAwarePredicate> pPlayer, DeserializationContext pDeserializationContext) {
      Optional<ContextAwarePredicate> optional = EntityPredicate.fromJson(pJson, "lightning", pDeserializationContext);
      Optional<ContextAwarePredicate> optional1 = EntityPredicate.fromJson(pJson, "bystander", pDeserializationContext);
      return new LightningStrikeTrigger.TriggerInstance(pPlayer, optional, optional1);
   }

   public void trigger(ServerPlayer pPlayer, LightningBolt pLightning, List<Entity> pNearbyEntities) {
      List<LootContext> list = pNearbyEntities.stream().map((p_153390_) -> {
         return EntityPredicate.createContext(pPlayer, p_153390_);
      }).collect(Collectors.toList());
      LootContext lootcontext = EntityPredicate.createContext(pPlayer, pLightning);
      this.trigger(pPlayer, (p_153402_) -> {
         return p_153402_.matches(lootcontext, list);
      });
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final Optional<ContextAwarePredicate> lightning;
      private final Optional<ContextAwarePredicate> bystander;

      public TriggerInstance(Optional<ContextAwarePredicate> pPlayer, Optional<ContextAwarePredicate> pLightning, Optional<ContextAwarePredicate> pBystander) {
         super(pPlayer);
         this.lightning = pLightning;
         this.bystander = pBystander;
      }

      public static Criterion<LightningStrikeTrigger.TriggerInstance> lightningStrike(Optional<EntityPredicate> pLightning, Optional<EntityPredicate> pBystander) {
         return CriteriaTriggers.LIGHTNING_STRIKE.createCriterion(new LightningStrikeTrigger.TriggerInstance(Optional.empty(), EntityPredicate.wrap(pLightning), EntityPredicate.wrap(pBystander)));
      }

      public boolean matches(LootContext pPlayerContext, List<LootContext> pEntityContexts) {
         if (this.lightning.isPresent() && !this.lightning.get().matches(pPlayerContext)) {
            return false;
         } else {
            return !this.bystander.isPresent() || !pEntityContexts.stream().noneMatch(this.bystander.get()::matches);
         }
      }

      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         this.lightning.ifPresent((p_297794_) -> {
            jsonobject.add("lightning", p_297794_.toJson());
         });
         this.bystander.ifPresent((p_297949_) -> {
            jsonobject.add("bystander", p_297949_.toJson());
         });
         return jsonobject;
      }
   }
}