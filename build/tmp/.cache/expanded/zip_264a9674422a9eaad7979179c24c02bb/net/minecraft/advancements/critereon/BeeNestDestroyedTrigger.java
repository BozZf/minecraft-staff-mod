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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class BeeNestDestroyedTrigger extends SimpleCriterionTrigger<BeeNestDestroyedTrigger.TriggerInstance> {
   public BeeNestDestroyedTrigger.TriggerInstance createInstance(JsonObject pJson, Optional<ContextAwarePredicate> pPlayer, DeserializationContext pDeserializationContext) {
      Block block = deserializeBlock(pJson);
      Optional<ItemPredicate> optional = ItemPredicate.fromJson(pJson.get("item"));
      MinMaxBounds.Ints minmaxbounds$ints = MinMaxBounds.Ints.fromJson(pJson.get("num_bees_inside"));
      return new BeeNestDestroyedTrigger.TriggerInstance(pPlayer, block, optional, minmaxbounds$ints);
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

   public void trigger(ServerPlayer pPlayer, BlockState pState, ItemStack pStack, int pNumBees) {
      this.trigger(pPlayer, (p_146660_) -> {
         return p_146660_.matches(pState, pStack, pNumBees);
      });
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      @Nullable
      private final Block block;
      private final Optional<ItemPredicate> item;
      private final MinMaxBounds.Ints numBees;

      public TriggerInstance(Optional<ContextAwarePredicate> pPlayer, @Nullable Block pBlock, Optional<ItemPredicate> pItem, MinMaxBounds.Ints pNumBees) {
         super(pPlayer);
         this.block = pBlock;
         this.item = pItem;
         this.numBees = pNumBees;
      }

      public static Criterion<BeeNestDestroyedTrigger.TriggerInstance> destroyedBeeNest(Block pBlock, ItemPredicate.Builder pItem, MinMaxBounds.Ints pNumBees) {
         return CriteriaTriggers.BEE_NEST_DESTROYED.createCriterion(new BeeNestDestroyedTrigger.TriggerInstance(Optional.empty(), pBlock, Optional.of(pItem.build()), pNumBees));
      }

      public boolean matches(BlockState pState, ItemStack pStack, int pNumBees) {
         if (this.block != null && !pState.is(this.block)) {
            return false;
         } else {
            return this.item.isPresent() && !this.item.get().matches(pStack) ? false : this.numBees.matches(pNumBees);
         }
      }

      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         if (this.block != null) {
            jsonobject.addProperty("block", BuiltInRegistries.BLOCK.getKey(this.block).toString());
         }

         this.item.ifPresent((p_297222_) -> {
            jsonobject.add("item", p_297222_.serializeToJson());
         });
         jsonobject.add("num_bees_inside", this.numBees.serializeToJson());
         return jsonobject;
      }
   }
}