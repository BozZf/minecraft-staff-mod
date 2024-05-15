package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.storage.loot.LootContext;

public class CuredZombieVillagerTrigger extends SimpleCriterionTrigger<CuredZombieVillagerTrigger.TriggerInstance> {
   public CuredZombieVillagerTrigger.TriggerInstance createInstance(JsonObject pJson, Optional<ContextAwarePredicate> pPlayer, DeserializationContext pDeserializationContext) {
      Optional<ContextAwarePredicate> optional = EntityPredicate.fromJson(pJson, "zombie", pDeserializationContext);
      Optional<ContextAwarePredicate> optional1 = EntityPredicate.fromJson(pJson, "villager", pDeserializationContext);
      return new CuredZombieVillagerTrigger.TriggerInstance(pPlayer, optional, optional1);
   }

   public void trigger(ServerPlayer pPlayer, Zombie pZombie, Villager pVillager) {
      LootContext lootcontext = EntityPredicate.createContext(pPlayer, pZombie);
      LootContext lootcontext1 = EntityPredicate.createContext(pPlayer, pVillager);
      this.trigger(pPlayer, (p_24285_) -> {
         return p_24285_.matches(lootcontext, lootcontext1);
      });
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final Optional<ContextAwarePredicate> zombie;
      private final Optional<ContextAwarePredicate> villager;

      public TriggerInstance(Optional<ContextAwarePredicate> pPlayer, Optional<ContextAwarePredicate> pZombie, Optional<ContextAwarePredicate> pVillager) {
         super(pPlayer);
         this.zombie = pZombie;
         this.villager = pVillager;
      }

      public static Criterion<CuredZombieVillagerTrigger.TriggerInstance> curedZombieVillager() {
         return CriteriaTriggers.CURED_ZOMBIE_VILLAGER.createCriterion(new CuredZombieVillagerTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.empty()));
      }

      public boolean matches(LootContext pZombie, LootContext pVillager) {
         if (this.zombie.isPresent() && !this.zombie.get().matches(pZombie)) {
            return false;
         } else {
            return !this.villager.isPresent() || this.villager.get().matches(pVillager);
         }
      }

      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         this.zombie.ifPresent((p_300843_) -> {
            jsonobject.add("zombie", p_300843_.toJson());
         });
         this.villager.ifPresent((p_300914_) -> {
            jsonobject.add("villager", p_300914_.toJson());
         });
         return jsonobject;
      }
   }
}