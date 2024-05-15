package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;

public class KilledTrigger extends SimpleCriterionTrigger<KilledTrigger.TriggerInstance> {
   public KilledTrigger.TriggerInstance createInstance(JsonObject pJson, Optional<ContextAwarePredicate> pPlayer, DeserializationContext pDeserializationContext) {
      return new KilledTrigger.TriggerInstance(pPlayer, EntityPredicate.fromJson(pJson, "entity", pDeserializationContext), DamageSourcePredicate.fromJson(pJson.get("killing_blow")));
   }

   public void trigger(ServerPlayer pPlayer, Entity pEntity, DamageSource pSource) {
      LootContext lootcontext = EntityPredicate.createContext(pPlayer, pEntity);
      this.trigger(pPlayer, (p_48112_) -> {
         return p_48112_.matches(pPlayer, lootcontext, pSource);
      });
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final Optional<ContextAwarePredicate> entityPredicate;
      private final Optional<DamageSourcePredicate> killingBlow;

      public TriggerInstance(Optional<ContextAwarePredicate> pPlayer, Optional<ContextAwarePredicate> pEntityPredicate, Optional<DamageSourcePredicate> pKillingBlow) {
         super(pPlayer);
         this.entityPredicate = pEntityPredicate;
         this.killingBlow = pKillingBlow;
      }

      public static Criterion<KilledTrigger.TriggerInstance> playerKilledEntity(Optional<EntityPredicate> pEntityPredicate) {
         return CriteriaTriggers.PLAYER_KILLED_ENTITY.createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), EntityPredicate.wrap(pEntityPredicate), Optional.empty()));
      }

      public static Criterion<KilledTrigger.TriggerInstance> playerKilledEntity(EntityPredicate.Builder pEntityPredicate) {
         return CriteriaTriggers.PLAYER_KILLED_ENTITY.createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), Optional.of(EntityPredicate.wrap(pEntityPredicate)), Optional.empty()));
      }

      public static Criterion<KilledTrigger.TriggerInstance> playerKilledEntity() {
         return CriteriaTriggers.PLAYER_KILLED_ENTITY.createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.empty()));
      }

      public static Criterion<KilledTrigger.TriggerInstance> playerKilledEntity(Optional<EntityPredicate> pEntityPredicate, Optional<DamageSourcePredicate> pKillingBlow) {
         return CriteriaTriggers.PLAYER_KILLED_ENTITY.createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), EntityPredicate.wrap(pEntityPredicate), pKillingBlow));
      }

      public static Criterion<KilledTrigger.TriggerInstance> playerKilledEntity(EntityPredicate.Builder pEntityPredicate, Optional<DamageSourcePredicate> pKillingBlow) {
         return CriteriaTriggers.PLAYER_KILLED_ENTITY.createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), Optional.of(EntityPredicate.wrap(pEntityPredicate)), pKillingBlow));
      }

      public static Criterion<KilledTrigger.TriggerInstance> playerKilledEntity(Optional<EntityPredicate> pEntityPredicate, DamageSourcePredicate.Builder pKillingBlow) {
         return CriteriaTriggers.PLAYER_KILLED_ENTITY.createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), EntityPredicate.wrap(pEntityPredicate), Optional.of(pKillingBlow.build())));
      }

      public static Criterion<KilledTrigger.TriggerInstance> playerKilledEntity(EntityPredicate.Builder pEntityPredicate, DamageSourcePredicate.Builder pKillingBlow) {
         return CriteriaTriggers.PLAYER_KILLED_ENTITY.createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), Optional.of(EntityPredicate.wrap(pEntityPredicate)), Optional.of(pKillingBlow.build())));
      }

      public static Criterion<KilledTrigger.TriggerInstance> playerKilledEntityNearSculkCatalyst() {
         return CriteriaTriggers.KILL_MOB_NEAR_SCULK_CATALYST.createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.empty()));
      }

      public static Criterion<KilledTrigger.TriggerInstance> entityKilledPlayer(Optional<EntityPredicate> pEntityPredicate) {
         return CriteriaTriggers.ENTITY_KILLED_PLAYER.createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), EntityPredicate.wrap(pEntityPredicate), Optional.empty()));
      }

      public static Criterion<KilledTrigger.TriggerInstance> entityKilledPlayer(EntityPredicate.Builder pEntityPredicate) {
         return CriteriaTriggers.ENTITY_KILLED_PLAYER.createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), Optional.of(EntityPredicate.wrap(pEntityPredicate)), Optional.empty()));
      }

      public static Criterion<KilledTrigger.TriggerInstance> entityKilledPlayer() {
         return CriteriaTriggers.ENTITY_KILLED_PLAYER.createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.empty()));
      }

      public static Criterion<KilledTrigger.TriggerInstance> entityKilledPlayer(Optional<EntityPredicate> pEntityPredicate, Optional<DamageSourcePredicate> pKillingBlow) {
         return CriteriaTriggers.ENTITY_KILLED_PLAYER.createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), EntityPredicate.wrap(pEntityPredicate), pKillingBlow));
      }

      public static Criterion<KilledTrigger.TriggerInstance> entityKilledPlayer(EntityPredicate.Builder pEntityPredicate, Optional<DamageSourcePredicate> pKillingBlow) {
         return CriteriaTriggers.ENTITY_KILLED_PLAYER.createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), Optional.of(EntityPredicate.wrap(pEntityPredicate)), pKillingBlow));
      }

      public static Criterion<KilledTrigger.TriggerInstance> entityKilledPlayer(Optional<EntityPredicate> pEntityPredicate, DamageSourcePredicate.Builder pKillingBlow) {
         return CriteriaTriggers.ENTITY_KILLED_PLAYER.createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), EntityPredicate.wrap(pEntityPredicate), Optional.of(pKillingBlow.build())));
      }

      public static Criterion<KilledTrigger.TriggerInstance> entityKilledPlayer(EntityPredicate.Builder pEntityPredicate, DamageSourcePredicate.Builder pKillingBlow) {
         return CriteriaTriggers.ENTITY_KILLED_PLAYER.createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), Optional.of(EntityPredicate.wrap(pEntityPredicate)), Optional.of(pKillingBlow.build())));
      }

      public boolean matches(ServerPlayer pPlayer, LootContext pContext, DamageSource pSource) {
         if (this.killingBlow.isPresent() && !this.killingBlow.get().matches(pPlayer, pSource)) {
            return false;
         } else {
            return this.entityPredicate.isEmpty() || this.entityPredicate.get().matches(pContext);
         }
      }

      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         this.entityPredicate.ifPresent((p_301388_) -> {
            jsonobject.add("entity", p_301388_.toJson());
         });
         this.killingBlow.ifPresent((p_299547_) -> {
            jsonobject.add("killing_blow", p_299547_.serializeToJson());
         });
         return jsonobject;
      }
   }
}