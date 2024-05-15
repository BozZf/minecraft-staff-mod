package net.minecraft.advancements.critereon;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditions;

public class ContextAwarePredicate {
   private final List<LootItemCondition> conditions;
   private final Predicate<LootContext> compositePredicates;

   ContextAwarePredicate(List<LootItemCondition> pConditions) {
      if (pConditions.isEmpty()) {
         throw new IllegalArgumentException("ContextAwarePredicate must have at least one condition");
      } else {
         this.conditions = pConditions;
         this.compositePredicates = LootItemConditions.andConditions(pConditions);
      }
   }

   public static ContextAwarePredicate create(LootItemCondition... pConditions) {
      return new ContextAwarePredicate(List.of(pConditions));
   }

   public static Optional<Optional<ContextAwarePredicate>> fromElement(String p_286647_, DeserializationContext pContext, @Nullable JsonElement pJson, LootContextParamSet pLootContextParams) {
      if (pJson != null && pJson.isJsonArray()) {
         List<LootItemCondition> list = pContext.deserializeConditions(pJson.getAsJsonArray(), pContext.getAdvancementId() + "/" + p_286647_, pLootContextParams);
         return list.isEmpty() ? Optional.of(Optional.empty()) : Optional.of(Optional.of(new ContextAwarePredicate(list)));
      } else {
         return Optional.empty();
      }
   }

   public boolean matches(LootContext pContext) {
      return this.compositePredicates.test(pContext);
   }

   public JsonElement toJson() {
      return Util.getOrThrow(LootItemConditions.CODEC.listOf().encodeStart(JsonOps.INSTANCE, this.conditions), IllegalStateException::new);
   }

   public static JsonElement toJson(List<ContextAwarePredicate> pPredicates) {
      if (pPredicates.isEmpty()) {
         return JsonNull.INSTANCE;
      } else {
         JsonArray jsonarray = new JsonArray();

         for(ContextAwarePredicate contextawarepredicate : pPredicates) {
            jsonarray.add(contextawarepredicate.toJson());
         }

         return jsonarray;
      }
   }
}