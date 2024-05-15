package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;

public class EffectsChangedTrigger extends SimpleCriterionTrigger<EffectsChangedTrigger.TriggerInstance> {
   public EffectsChangedTrigger.TriggerInstance createInstance(JsonObject pJson, Optional<ContextAwarePredicate> pPlayer, DeserializationContext pDeserializationContext) {
      Optional<MobEffectsPredicate> optional = MobEffectsPredicate.fromJson(pJson.get("effects"));
      Optional<ContextAwarePredicate> optional1 = EntityPredicate.fromJson(pJson, "source", pDeserializationContext);
      return new EffectsChangedTrigger.TriggerInstance(pPlayer, optional, optional1);
   }

   public void trigger(ServerPlayer pPlayer, @Nullable Entity pSource) {
      LootContext lootcontext = pSource != null ? EntityPredicate.createContext(pPlayer, pSource) : null;
      this.trigger(pPlayer, (p_149268_) -> {
         return p_149268_.matches(pPlayer, lootcontext);
      });
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final Optional<MobEffectsPredicate> effects;
      private final Optional<ContextAwarePredicate> source;

      public TriggerInstance(Optional<ContextAwarePredicate> pPlayer, Optional<MobEffectsPredicate> pEffects, Optional<ContextAwarePredicate> pSource) {
         super(pPlayer);
         this.effects = pEffects;
         this.source = pSource;
      }

      public static Criterion<EffectsChangedTrigger.TriggerInstance> hasEffects(MobEffectsPredicate.Builder pEffects) {
         return CriteriaTriggers.EFFECTS_CHANGED.createCriterion(new EffectsChangedTrigger.TriggerInstance(Optional.empty(), pEffects.build(), Optional.empty()));
      }

      public static Criterion<EffectsChangedTrigger.TriggerInstance> gotEffectsFrom(EntityPredicate.Builder pSource) {
         return CriteriaTriggers.EFFECTS_CHANGED.createCriterion(new EffectsChangedTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.of(EntityPredicate.wrap(pSource.build()))));
      }

      public boolean matches(ServerPlayer pPlayer, @Nullable LootContext pLootContext) {
         if (this.effects.isPresent() && !this.effects.get().matches(pPlayer)) {
            return false;
         } else {
            return !this.source.isPresent() || pLootContext != null && this.source.get().matches(pLootContext);
         }
      }

      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         this.effects.ifPresent((p_297869_) -> {
            jsonobject.add("effects", p_297869_.serializeToJson());
         });
         this.source.ifPresent((p_298372_) -> {
            jsonobject.add("source", p_298372_.toJson());
         });
         return jsonobject;
      }
   }
}