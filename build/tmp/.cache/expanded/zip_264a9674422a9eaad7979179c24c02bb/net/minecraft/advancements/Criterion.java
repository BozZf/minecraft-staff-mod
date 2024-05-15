package net.minecraft.advancements;

import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.Map;
import java.util.Objects;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

public record Criterion<T extends CriterionTriggerInstance>(CriterionTrigger<T> trigger, T triggerInstance) {
   public static Criterion<?> criterionFromJson(JsonObject pJson, DeserializationContext pContext) {
      ResourceLocation resourcelocation = new ResourceLocation(GsonHelper.getAsString(pJson, "trigger"));
      CriterionTrigger<?> criteriontrigger = CriteriaTriggers.getCriterion(resourcelocation);
      if (criteriontrigger == null) {
         throw new JsonSyntaxException("Invalid criterion trigger: " + resourcelocation);
      } else {
         return criterionFromJson(pJson, pContext, criteriontrigger);
      }
   }

   private static <T extends CriterionTriggerInstance> Criterion<T> criterionFromJson(JsonObject pJson, DeserializationContext pDeserializationContext, CriterionTrigger<T> pTrigger) {
      T t = pTrigger.createInstance(GsonHelper.getAsJsonObject(pJson, "conditions", new JsonObject()), pDeserializationContext);
      return new Criterion<>(pTrigger, t);
   }

   public static Map<String, Criterion<?>> criteriaFromJson(JsonObject pJson, DeserializationContext pContext) {
      Map<String, Criterion<?>> map = Maps.newHashMap();

      for(Map.Entry<String, JsonElement> entry : pJson.entrySet()) {
         map.put(entry.getKey(), criterionFromJson(GsonHelper.convertToJsonObject(entry.getValue(), "criterion"), pContext));
      }

      return map;
   }

   public JsonElement serializeToJson() {
      JsonObject jsonobject = new JsonObject();
      jsonobject.addProperty("trigger", Objects.requireNonNull(CriteriaTriggers.getId(this.trigger), "Unregistered trigger").toString());
      JsonObject jsonobject1 = this.triggerInstance.serializeToJson();
      if (jsonobject1.size() != 0) {
         jsonobject.add("conditions", jsonobject1);
      }

      return jsonobject;
   }
}