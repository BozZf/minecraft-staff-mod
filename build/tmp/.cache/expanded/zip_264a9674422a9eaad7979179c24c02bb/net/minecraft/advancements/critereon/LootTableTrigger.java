package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;

public class LootTableTrigger extends SimpleCriterionTrigger<LootTableTrigger.TriggerInstance> {
   protected LootTableTrigger.TriggerInstance createInstance(JsonObject pJson, Optional<ContextAwarePredicate> pPlayer, DeserializationContext pDeserializationContext) {
      ResourceLocation resourcelocation = new ResourceLocation(GsonHelper.getAsString(pJson, "loot_table"));
      return new LootTableTrigger.TriggerInstance(pPlayer, resourcelocation);
   }

   public void trigger(ServerPlayer pPlayer, ResourceLocation pLootTable) {
      this.trigger(pPlayer, (p_54606_) -> {
         return p_54606_.matches(pLootTable);
      });
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final ResourceLocation lootTable;

      public TriggerInstance(Optional<ContextAwarePredicate> pPlayer, ResourceLocation pLootTable) {
         super(pPlayer);
         this.lootTable = pLootTable;
      }

      public static Criterion<LootTableTrigger.TriggerInstance> lootTableUsed(ResourceLocation pLootTable) {
         return CriteriaTriggers.GENERATE_LOOT.createCriterion(new LootTableTrigger.TriggerInstance(Optional.empty(), pLootTable));
      }

      public boolean matches(ResourceLocation pLootTable) {
         return this.lootTable.equals(pLootTable);
      }

      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         jsonobject.addProperty("loot_table", this.lootTable.toString());
         return jsonobject;
      }
   }
}