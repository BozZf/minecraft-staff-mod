package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.server.PlayerAdvancements;

public class ImpossibleTrigger implements CriterionTrigger<ImpossibleTrigger.TriggerInstance> {
   public void addPlayerListener(PlayerAdvancements pPlayerAdvancements, CriterionTrigger.Listener<ImpossibleTrigger.TriggerInstance> pListener) {
   }

   public void removePlayerListener(PlayerAdvancements pPlayerAdvancements, CriterionTrigger.Listener<ImpossibleTrigger.TriggerInstance> pListener) {
   }

   public void removePlayerListeners(PlayerAdvancements pPlayerAdvancements) {
   }

   public ImpossibleTrigger.TriggerInstance createInstance(JsonObject pObject, DeserializationContext pConditions) {
      return new ImpossibleTrigger.TriggerInstance();
   }

   public static class TriggerInstance implements CriterionTriggerInstance {
      public JsonObject serializeToJson() {
         return new JsonObject();
      }
   }
}