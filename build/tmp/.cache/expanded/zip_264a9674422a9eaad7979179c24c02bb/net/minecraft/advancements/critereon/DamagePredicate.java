package net.minecraft.advancements.critereon;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.damagesource.DamageSource;

public record DamagePredicate(MinMaxBounds.Doubles dealtDamage, MinMaxBounds.Doubles takenDamage, Optional<EntityPredicate> sourceEntity, Optional<Boolean> blocked, Optional<DamageSourcePredicate> type) {
   public boolean matches(ServerPlayer pPlayer, DamageSource pSource, float pDealtDamage, float pTakenDamage, boolean pBlocked) {
      if (!this.dealtDamage.matches((double)pDealtDamage)) {
         return false;
      } else if (!this.takenDamage.matches((double)pTakenDamage)) {
         return false;
      } else if (this.sourceEntity.isPresent() && !this.sourceEntity.get().matches(pPlayer, pSource.getEntity())) {
         return false;
      } else if (this.blocked.isPresent() && this.blocked.get() != pBlocked) {
         return false;
      } else {
         return !this.type.isPresent() || this.type.get().matches(pPlayer, pSource);
      }
   }

   public static Optional<DamagePredicate> fromJson(@Nullable JsonElement pJson) {
      if (pJson != null && !pJson.isJsonNull()) {
         JsonObject jsonobject = GsonHelper.convertToJsonObject(pJson, "damage");
         MinMaxBounds.Doubles minmaxbounds$doubles = MinMaxBounds.Doubles.fromJson(jsonobject.get("dealt"));
         MinMaxBounds.Doubles minmaxbounds$doubles1 = MinMaxBounds.Doubles.fromJson(jsonobject.get("taken"));
         Optional<Boolean> optional = jsonobject.has("blocked") ? Optional.of(GsonHelper.getAsBoolean(jsonobject, "blocked")) : Optional.empty();
         Optional<EntityPredicate> optional1 = EntityPredicate.fromJson(jsonobject.get("source_entity"));
         Optional<DamageSourcePredicate> optional2 = DamageSourcePredicate.fromJson(jsonobject.get("type"));
         return minmaxbounds$doubles.isAny() && minmaxbounds$doubles1.isAny() && optional1.isEmpty() && optional.isEmpty() && optional2.isEmpty() ? Optional.empty() : Optional.of(new DamagePredicate(minmaxbounds$doubles, minmaxbounds$doubles1, optional1, optional, optional2));
      } else {
         return Optional.empty();
      }
   }

   public JsonElement serializeToJson() {
      JsonObject jsonobject = new JsonObject();
      jsonobject.add("dealt", this.dealtDamage.serializeToJson());
      jsonobject.add("taken", this.takenDamage.serializeToJson());
      this.sourceEntity.ifPresent((p_301108_) -> {
         jsonobject.add("source_entity", p_301108_.serializeToJson());
      });
      this.type.ifPresent((p_301317_) -> {
         jsonobject.add("type", p_301317_.serializeToJson());
      });
      this.blocked.ifPresent((p_299037_) -> {
         jsonobject.addProperty("blocked", p_299037_);
      });
      return jsonobject;
   }

   public static class Builder {
      private MinMaxBounds.Doubles dealtDamage = MinMaxBounds.Doubles.ANY;
      private MinMaxBounds.Doubles takenDamage = MinMaxBounds.Doubles.ANY;
      private Optional<EntityPredicate> sourceEntity = Optional.empty();
      private Optional<Boolean> blocked = Optional.empty();
      private Optional<DamageSourcePredicate> type = Optional.empty();

      public static DamagePredicate.Builder damageInstance() {
         return new DamagePredicate.Builder();
      }

      public DamagePredicate.Builder dealtDamage(MinMaxBounds.Doubles pDealtDamage) {
         this.dealtDamage = pDealtDamage;
         return this;
      }

      public DamagePredicate.Builder takenDamage(MinMaxBounds.Doubles pTakenDamage) {
         this.takenDamage = pTakenDamage;
         return this;
      }

      public DamagePredicate.Builder sourceEntity(EntityPredicate pSourceEntity) {
         this.sourceEntity = Optional.of(pSourceEntity);
         return this;
      }

      public DamagePredicate.Builder blocked(Boolean pBlocked) {
         this.blocked = Optional.of(pBlocked);
         return this;
      }

      public DamagePredicate.Builder type(DamageSourcePredicate pType) {
         this.type = Optional.of(pType);
         return this;
      }

      public DamagePredicate.Builder type(DamageSourcePredicate.Builder pTypeBuilder) {
         this.type = Optional.of(pTypeBuilder.build());
         return this;
      }

      public DamagePredicate build() {
         return new DamagePredicate(this.dealtDamage, this.takenDamage, this.sourceEntity, this.blocked, this.type);
      }
   }
}