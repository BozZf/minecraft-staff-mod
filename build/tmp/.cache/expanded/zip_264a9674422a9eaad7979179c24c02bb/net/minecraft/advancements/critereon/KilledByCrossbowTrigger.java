package net.minecraft.advancements.critereon;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.loot.LootContext;

public class KilledByCrossbowTrigger extends SimpleCriterionTrigger<KilledByCrossbowTrigger.TriggerInstance> {
   public KilledByCrossbowTrigger.TriggerInstance createInstance(JsonObject pJson, Optional<ContextAwarePredicate> pPlayer, DeserializationContext pDeserializationContext) {
      List<ContextAwarePredicate> list = EntityPredicate.fromJsonArray(pJson, "victims", pDeserializationContext);
      MinMaxBounds.Ints minmaxbounds$ints = MinMaxBounds.Ints.fromJson(pJson.get("unique_entity_types"));
      return new KilledByCrossbowTrigger.TriggerInstance(pPlayer, list, minmaxbounds$ints);
   }

   public void trigger(ServerPlayer pPlayer, Collection<Entity> pEntities) {
      List<LootContext> list = Lists.newArrayList();
      Set<EntityType<?>> set = Sets.newHashSet();

      for(Entity entity : pEntities) {
         set.add(entity.getType());
         list.add(EntityPredicate.createContext(pPlayer, entity));
      }

      this.trigger(pPlayer, (p_46881_) -> {
         return p_46881_.matches(list, set.size());
      });
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final List<ContextAwarePredicate> victims;
      private final MinMaxBounds.Ints uniqueEntityTypes;

      public TriggerInstance(Optional<ContextAwarePredicate> pPlayer, List<ContextAwarePredicate> pVictims, MinMaxBounds.Ints pUniqueEntityTypes) {
         super(pPlayer);
         this.victims = pVictims;
         this.uniqueEntityTypes = pUniqueEntityTypes;
      }

      public static Criterion<KilledByCrossbowTrigger.TriggerInstance> crossbowKilled(EntityPredicate.Builder... pVictims) {
         return CriteriaTriggers.KILLED_BY_CROSSBOW.createCriterion(new KilledByCrossbowTrigger.TriggerInstance(Optional.empty(), EntityPredicate.wrap(pVictims), MinMaxBounds.Ints.ANY));
      }

      public static Criterion<KilledByCrossbowTrigger.TriggerInstance> crossbowKilled(MinMaxBounds.Ints pUniqueEntityTypes) {
         return CriteriaTriggers.KILLED_BY_CROSSBOW.createCriterion(new KilledByCrossbowTrigger.TriggerInstance(Optional.empty(), List.of(), pUniqueEntityTypes));
      }

      public boolean matches(Collection<LootContext> pContexts, int pBounds) {
         if (!this.victims.isEmpty()) {
            List<LootContext> list = Lists.newArrayList(pContexts);

            for(ContextAwarePredicate contextawarepredicate : this.victims) {
               boolean flag = false;
               Iterator<LootContext> iterator = list.iterator();

               while(iterator.hasNext()) {
                  LootContext lootcontext = iterator.next();
                  if (contextawarepredicate.matches(lootcontext)) {
                     iterator.remove();
                     flag = true;
                     break;
                  }
               }

               if (!flag) {
                  return false;
               }
            }
         }

         return this.uniqueEntityTypes.matches(pBounds);
      }

      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         jsonobject.add("victims", ContextAwarePredicate.toJson(this.victims));
         jsonobject.add("unique_entity_types", this.uniqueEntityTypes.serializeToJson());
         return jsonobject;
      }
   }
}