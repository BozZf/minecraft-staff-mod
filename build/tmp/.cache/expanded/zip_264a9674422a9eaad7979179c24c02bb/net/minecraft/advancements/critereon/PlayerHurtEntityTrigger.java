package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;

public class PlayerHurtEntityTrigger extends SimpleCriterionTrigger<PlayerHurtEntityTrigger.TriggerInstance> {
   public PlayerHurtEntityTrigger.TriggerInstance createInstance(JsonObject pJson, Optional<ContextAwarePredicate> pPlayer, DeserializationContext pDeserializationContext) {
      Optional<DamagePredicate> optional = DamagePredicate.fromJson(pJson.get("damage"));
      Optional<ContextAwarePredicate> optional1 = EntityPredicate.fromJson(pJson, "entity", pDeserializationContext);
      return new PlayerHurtEntityTrigger.TriggerInstance(pPlayer, optional, optional1);
   }

   public void trigger(ServerPlayer pPlayer, Entity pEntity, DamageSource pSource, float pAmountDealt, float pAmountTaken, boolean pBlocked) {
      LootContext lootcontext = EntityPredicate.createContext(pPlayer, pEntity);
      this.trigger(pPlayer, (p_60126_) -> {
         return p_60126_.matches(pPlayer, lootcontext, pSource, pAmountDealt, pAmountTaken, pBlocked);
      });
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final Optional<DamagePredicate> damage;
      private final Optional<ContextAwarePredicate> entity;

      public TriggerInstance(Optional<ContextAwarePredicate> pPlayer, Optional<DamagePredicate> pDamage, Optional<ContextAwarePredicate> pEntity) {
         super(pPlayer);
         this.damage = pDamage;
         this.entity = pEntity;
      }

      public static Criterion<PlayerHurtEntityTrigger.TriggerInstance> playerHurtEntity() {
         return CriteriaTriggers.PLAYER_HURT_ENTITY.createCriterion(new PlayerHurtEntityTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.empty()));
      }

      public static Criterion<PlayerHurtEntityTrigger.TriggerInstance> playerHurtEntityWithDamage(Optional<DamagePredicate> pDamage) {
         return CriteriaTriggers.PLAYER_HURT_ENTITY.createCriterion(new PlayerHurtEntityTrigger.TriggerInstance(Optional.empty(), pDamage, Optional.empty()));
      }

      public static Criterion<PlayerHurtEntityTrigger.TriggerInstance> playerHurtEntityWithDamage(DamagePredicate.Builder pDamage) {
         return CriteriaTriggers.PLAYER_HURT_ENTITY.createCriterion(new PlayerHurtEntityTrigger.TriggerInstance(Optional.empty(), Optional.of(pDamage.build()), Optional.empty()));
      }

      public static Criterion<PlayerHurtEntityTrigger.TriggerInstance> playerHurtEntity(Optional<EntityPredicate> pEntity) {
         return CriteriaTriggers.PLAYER_HURT_ENTITY.createCriterion(new PlayerHurtEntityTrigger.TriggerInstance(Optional.empty(), Optional.empty(), EntityPredicate.wrap(pEntity)));
      }

      public static Criterion<PlayerHurtEntityTrigger.TriggerInstance> playerHurtEntity(Optional<DamagePredicate> pDamage, Optional<EntityPredicate> pEntity) {
         return CriteriaTriggers.PLAYER_HURT_ENTITY.createCriterion(new PlayerHurtEntityTrigger.TriggerInstance(Optional.empty(), pDamage, EntityPredicate.wrap(pEntity)));
      }

      public static Criterion<PlayerHurtEntityTrigger.TriggerInstance> playerHurtEntity(DamagePredicate.Builder pDamage, Optional<EntityPredicate> pEntity) {
         return CriteriaTriggers.PLAYER_HURT_ENTITY.createCriterion(new PlayerHurtEntityTrigger.TriggerInstance(Optional.empty(), Optional.of(pDamage.build()), EntityPredicate.wrap(pEntity)));
      }

      public boolean matches(ServerPlayer pPlayer, LootContext pContext, DamageSource pDamage, float pDealt, float pTaken, boolean pBlocked) {
         if (this.damage.isPresent() && !this.damage.get().matches(pPlayer, pDamage, pDealt, pTaken, pBlocked)) {
            return false;
         } else {
            return !this.entity.isPresent() || this.entity.get().matches(pContext);
         }
      }

      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         this.damage.ifPresent((p_297324_) -> {
            jsonobject.add("damage", p_297324_.serializeToJson());
         });
         this.entity.ifPresent((p_298522_) -> {
            jsonobject.add("entity", p_298522_.toJson());
         });
         return jsonobject;
      }
   }
}