package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

public class EntityHurtPlayerTrigger extends SimpleCriterionTrigger<EntityHurtPlayerTrigger.TriggerInstance> {
   public EntityHurtPlayerTrigger.TriggerInstance createInstance(JsonObject pJson, Optional<ContextAwarePredicate> pPlayer, DeserializationContext pDeserializationContext) {
      Optional<DamagePredicate> optional = DamagePredicate.fromJson(pJson.get("damage"));
      return new EntityHurtPlayerTrigger.TriggerInstance(pPlayer, optional);
   }

   public void trigger(ServerPlayer pPlayer, DamageSource pSource, float pDealtDamage, float pTakenDamage, boolean pBlocked) {
      this.trigger(pPlayer, (p_35186_) -> {
         return p_35186_.matches(pPlayer, pSource, pDealtDamage, pTakenDamage, pBlocked);
      });
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final Optional<DamagePredicate> damage;

      public TriggerInstance(Optional<ContextAwarePredicate> pPlayer, Optional<DamagePredicate> pDamage) {
         super(pPlayer);
         this.damage = pDamage;
      }

      public static Criterion<EntityHurtPlayerTrigger.TriggerInstance> entityHurtPlayer() {
         return CriteriaTriggers.ENTITY_HURT_PLAYER.createCriterion(new EntityHurtPlayerTrigger.TriggerInstance(Optional.empty(), Optional.empty()));
      }

      public static Criterion<EntityHurtPlayerTrigger.TriggerInstance> entityHurtPlayer(DamagePredicate pDamage) {
         return CriteriaTriggers.ENTITY_HURT_PLAYER.createCriterion(new EntityHurtPlayerTrigger.TriggerInstance(Optional.empty(), Optional.of(pDamage)));
      }

      public static Criterion<EntityHurtPlayerTrigger.TriggerInstance> entityHurtPlayer(DamagePredicate.Builder pDamage) {
         return CriteriaTriggers.ENTITY_HURT_PLAYER.createCriterion(new EntityHurtPlayerTrigger.TriggerInstance(Optional.empty(), Optional.of(pDamage.build())));
      }

      public boolean matches(ServerPlayer pPlayer, DamageSource pSource, float pDealtDamage, float pTakenDamage, boolean pBlocked) {
         return !this.damage.isPresent() || this.damage.get().matches(pPlayer, pSource, pDealtDamage, pTakenDamage, pBlocked);
      }

      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         this.damage.ifPresent((p_297861_) -> {
            jsonobject.add("damage", p_297861_.serializeToJson());
         });
         return jsonobject;
      }
   }
}