package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class EnterBlockTrigger extends SimpleCriterionTrigger<EnterBlockTrigger.TriggerInstance> {
   public EnterBlockTrigger.TriggerInstance createInstance(JsonObject pJson, Optional<ContextAwarePredicate> pPlayer, DeserializationContext pDeserializationContext) {
      Block block = deserializeBlock(pJson);
      Optional<StatePropertiesPredicate> optional = StatePropertiesPredicate.fromJson(pJson.get("state"));
      if (block != null) {
         optional.ifPresent((p_296118_) -> {
            p_296118_.checkState(block.getStateDefinition(), (p_31274_) -> {
               throw new JsonSyntaxException("Block " + block + " has no property " + p_31274_);
            });
         });
      }

      return new EnterBlockTrigger.TriggerInstance(pPlayer, block, optional);
   }

   @Nullable
   private static Block deserializeBlock(JsonObject pJson) {
      if (pJson.has("block")) {
         ResourceLocation resourcelocation = new ResourceLocation(GsonHelper.getAsString(pJson, "block"));
         return BuiltInRegistries.BLOCK.getOptional(resourcelocation).orElseThrow(() -> {
            return new JsonSyntaxException("Unknown block type '" + resourcelocation + "'");
         });
      } else {
         return null;
      }
   }

   public void trigger(ServerPlayer pPlayer, BlockState pState) {
      this.trigger(pPlayer, (p_31277_) -> {
         return p_31277_.matches(pState);
      });
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      @Nullable
      private final Block block;
      private final Optional<StatePropertiesPredicate> state;

      public TriggerInstance(Optional<ContextAwarePredicate> pPlayer, @Nullable Block pBlock, Optional<StatePropertiesPredicate> pState) {
         super(pPlayer);
         this.block = pBlock;
         this.state = pState;
      }

      public static Criterion<EnterBlockTrigger.TriggerInstance> entersBlock(Block pBlock) {
         return CriteriaTriggers.ENTER_BLOCK.createCriterion(new EnterBlockTrigger.TriggerInstance(Optional.empty(), pBlock, Optional.empty()));
      }

      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         if (this.block != null) {
            jsonobject.addProperty("block", BuiltInRegistries.BLOCK.getKey(this.block).toString());
         }

         this.state.ifPresent((p_298367_) -> {
            jsonobject.add("state", p_298367_.serializeToJson());
         });
         return jsonobject;
      }

      public boolean matches(BlockState pState) {
         if (this.block != null && !pState.is(this.block)) {
            return false;
         } else {
            return !this.state.isPresent() || this.state.get().matches(pState);
         }
      }
   }
}