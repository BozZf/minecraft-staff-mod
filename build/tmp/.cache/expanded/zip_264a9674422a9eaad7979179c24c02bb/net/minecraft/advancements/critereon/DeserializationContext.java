package net.minecraft.advancements.critereon;

import com.google.gson.JsonArray;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootDataManager;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditions;
import org.slf4j.Logger;

public class DeserializationContext {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final ResourceLocation id;
   private final LootDataManager lootData;

   public DeserializationContext(ResourceLocation pId, LootDataManager pLootData) {
      this.id = pId;
      this.lootData = pLootData;
   }

   public final List<LootItemCondition> deserializeConditions(JsonArray pJson, String pLocation, LootContextParamSet pLootContextParams) {
      List<LootItemCondition> list = Util.getOrThrow(LootItemConditions.CODEC.listOf().parse(JsonOps.INSTANCE, pJson), JsonParseException::new);
      ValidationContext validationcontext = new ValidationContext(pLootContextParams, this.lootData);

      for(LootItemCondition lootitemcondition : list) {
         lootitemcondition.validate(validationcontext);
         validationcontext.getProblems().forEach((p_25880_, p_25881_) -> {
            LOGGER.warn("Found validation problem in advancement trigger {}/{}: {}", pLocation, p_25880_, p_25881_);
         });
      }

      return list;
   }

   public ResourceLocation getAdvancementId() {
      return this.id;
   }
}