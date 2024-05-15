package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.util.Arrays;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LocationCheck;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;

public class ItemUsedOnLocationTrigger extends SimpleCriterionTrigger<ItemUsedOnLocationTrigger.TriggerInstance> {
   public ItemUsedOnLocationTrigger.TriggerInstance createInstance(JsonObject pJson, Optional<ContextAwarePredicate> pPlayer, DeserializationContext pDeserializationContext) {
      Optional<Optional<ContextAwarePredicate>> optional = ContextAwarePredicate.fromElement("location", pDeserializationContext, pJson.get("location"), LootContextParamSets.ADVANCEMENT_LOCATION);
      if (optional.isEmpty()) {
         throw new JsonParseException("Failed to parse 'location' field");
      } else {
         return new ItemUsedOnLocationTrigger.TriggerInstance(pPlayer, optional.get());
      }
   }

   public void trigger(ServerPlayer pPlayer, BlockPos pPos, ItemStack pStack) {
      ServerLevel serverlevel = pPlayer.serverLevel();
      BlockState blockstate = serverlevel.getBlockState(pPos);
      LootParams lootparams = (new LootParams.Builder(serverlevel)).withParameter(LootContextParams.ORIGIN, pPos.getCenter()).withParameter(LootContextParams.THIS_ENTITY, pPlayer).withParameter(LootContextParams.BLOCK_STATE, blockstate).withParameter(LootContextParams.TOOL, pStack).create(LootContextParamSets.ADVANCEMENT_LOCATION);
      LootContext lootcontext = (new LootContext.Builder(lootparams)).create(Optional.empty());
      this.trigger(pPlayer, (p_286596_) -> {
         return p_286596_.matches(lootcontext);
      });
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final Optional<ContextAwarePredicate> location;

      public TriggerInstance(Optional<ContextAwarePredicate> pPlayer, Optional<ContextAwarePredicate> pLocation) {
         super(pPlayer);
         this.location = pLocation;
      }

      public static Criterion<ItemUsedOnLocationTrigger.TriggerInstance> placedBlock(Block pBlock) {
         ContextAwarePredicate contextawarepredicate = ContextAwarePredicate.create(LootItemBlockStatePropertyCondition.hasBlockStateProperties(pBlock).build());
         return CriteriaTriggers.PLACED_BLOCK.createCriterion(new ItemUsedOnLocationTrigger.TriggerInstance(Optional.empty(), Optional.of(contextawarepredicate)));
      }

      public static Criterion<ItemUsedOnLocationTrigger.TriggerInstance> placedBlock(LootItemCondition.Builder... pConditions) {
         ContextAwarePredicate contextawarepredicate = ContextAwarePredicate.create(Arrays.stream(pConditions).map(LootItemCondition.Builder::build).toArray((p_286827_) -> {
            return new LootItemCondition[p_286827_];
         }));
         return CriteriaTriggers.PLACED_BLOCK.createCriterion(new ItemUsedOnLocationTrigger.TriggerInstance(Optional.empty(), Optional.of(contextawarepredicate)));
      }

      private static ItemUsedOnLocationTrigger.TriggerInstance itemUsedOnLocation(LocationPredicate.Builder pLocation, ItemPredicate.Builder pTool) {
         ContextAwarePredicate contextawarepredicate = ContextAwarePredicate.create(LocationCheck.checkLocation(pLocation).build(), MatchTool.toolMatches(pTool).build());
         return new ItemUsedOnLocationTrigger.TriggerInstance(Optional.empty(), Optional.of(contextawarepredicate));
      }

      public static Criterion<ItemUsedOnLocationTrigger.TriggerInstance> itemUsedOnBlock(LocationPredicate.Builder pLocation, ItemPredicate.Builder pTool) {
         return CriteriaTriggers.ITEM_USED_ON_BLOCK.createCriterion(itemUsedOnLocation(pLocation, pTool));
      }

      public static Criterion<ItemUsedOnLocationTrigger.TriggerInstance> allayDropItemOnBlock(LocationPredicate.Builder pLocation, ItemPredicate.Builder pTool) {
         return CriteriaTriggers.ALLAY_DROP_ITEM_ON_BLOCK.createCriterion(itemUsedOnLocation(pLocation, pTool));
      }

      public boolean matches(LootContext pContext) {
         return this.location.isEmpty() || this.location.get().matches(pContext);
      }

      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         this.location.ifPresent((p_296136_) -> {
            jsonobject.add("location", p_296136_.toJson());
         });
         return jsonobject;
      }
   }
}