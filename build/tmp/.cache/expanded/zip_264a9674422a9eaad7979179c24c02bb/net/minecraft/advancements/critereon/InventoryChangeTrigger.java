package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.core.HolderSet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public class InventoryChangeTrigger extends SimpleCriterionTrigger<InventoryChangeTrigger.TriggerInstance> {
   public InventoryChangeTrigger.TriggerInstance createInstance(JsonObject pJson, Optional<ContextAwarePredicate> pPlayer, DeserializationContext pDeserializationContext) {
      JsonObject jsonobject = GsonHelper.getAsJsonObject(pJson, "slots", new JsonObject());
      MinMaxBounds.Ints minmaxbounds$ints = MinMaxBounds.Ints.fromJson(jsonobject.get("occupied"));
      MinMaxBounds.Ints minmaxbounds$ints1 = MinMaxBounds.Ints.fromJson(jsonobject.get("full"));
      MinMaxBounds.Ints minmaxbounds$ints2 = MinMaxBounds.Ints.fromJson(jsonobject.get("empty"));
      List<ItemPredicate> list = ItemPredicate.fromJsonArray(pJson.get("items"));
      return new InventoryChangeTrigger.TriggerInstance(pPlayer, minmaxbounds$ints, minmaxbounds$ints1, minmaxbounds$ints2, list);
   }

   public void trigger(ServerPlayer pPlayer, Inventory pInventory, ItemStack pStack) {
      int i = 0;
      int j = 0;
      int k = 0;

      for(int l = 0; l < pInventory.getContainerSize(); ++l) {
         ItemStack itemstack = pInventory.getItem(l);
         if (itemstack.isEmpty()) {
            ++j;
         } else {
            ++k;
            if (itemstack.getCount() >= itemstack.getMaxStackSize()) {
               ++i;
            }
         }
      }

      this.trigger(pPlayer, pInventory, pStack, i, j, k);
   }

   private void trigger(ServerPlayer pPlayer, Inventory pInventory, ItemStack pStack, int pFull, int pEmpty, int pOccupied) {
      this.trigger(pPlayer, (p_43166_) -> {
         return p_43166_.matches(pInventory, pStack, pFull, pEmpty, pOccupied);
      });
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final MinMaxBounds.Ints slotsOccupied;
      private final MinMaxBounds.Ints slotsFull;
      private final MinMaxBounds.Ints slotsEmpty;
      private final List<ItemPredicate> predicates;

      public TriggerInstance(Optional<ContextAwarePredicate> pPlayer, MinMaxBounds.Ints pSlotsOccupied, MinMaxBounds.Ints pSlotsFull, MinMaxBounds.Ints pSlotsEmpty, List<ItemPredicate> pPredicates) {
         super(pPlayer);
         this.slotsOccupied = pSlotsOccupied;
         this.slotsFull = pSlotsFull;
         this.slotsEmpty = pSlotsEmpty;
         this.predicates = pPredicates;
      }

      public static Criterion<InventoryChangeTrigger.TriggerInstance> hasItems(ItemPredicate.Builder... pItems) {
         return hasItems((ItemPredicate[])Stream.of(pItems).map(ItemPredicate.Builder::build).toArray((int p_296132_) -> {
            return new ItemPredicate[p_296132_];
         }));
      }

      public static Criterion<InventoryChangeTrigger.TriggerInstance> hasItems(ItemPredicate... pItems) {
         return CriteriaTriggers.INVENTORY_CHANGED.createCriterion(new InventoryChangeTrigger.TriggerInstance(Optional.empty(), MinMaxBounds.Ints.ANY, MinMaxBounds.Ints.ANY, MinMaxBounds.Ints.ANY, List.of(pItems)));
      }

      public static Criterion<InventoryChangeTrigger.TriggerInstance> hasItems(ItemLike... pItems) {
         ItemPredicate[] aitempredicate = new ItemPredicate[pItems.length];

         for(int i = 0; i < pItems.length; ++i) {
            aitempredicate[i] = new ItemPredicate(Optional.empty(), Optional.of(HolderSet.direct(pItems[i].asItem().builtInRegistryHolder())), MinMaxBounds.Ints.ANY, MinMaxBounds.Ints.ANY, List.of(), List.of(), Optional.empty(), Optional.empty());
         }

         return hasItems(aitempredicate);
      }

      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         if (!this.slotsOccupied.isAny() || !this.slotsFull.isAny() || !this.slotsEmpty.isAny()) {
            JsonObject jsonobject1 = new JsonObject();
            jsonobject1.add("occupied", this.slotsOccupied.serializeToJson());
            jsonobject1.add("full", this.slotsFull.serializeToJson());
            jsonobject1.add("empty", this.slotsEmpty.serializeToJson());
            jsonobject.add("slots", jsonobject1);
         }

         if (!this.predicates.isEmpty()) {
            jsonobject.add("items", ItemPredicate.serializeToJsonArray(this.predicates));
         }

         return jsonobject;
      }

      public boolean matches(Inventory pInventory, ItemStack pStack, int pFull, int pEmpty, int pOccupied) {
         if (!this.slotsFull.matches(pFull)) {
            return false;
         } else if (!this.slotsEmpty.matches(pEmpty)) {
            return false;
         } else if (!this.slotsOccupied.matches(pOccupied)) {
            return false;
         } else if (this.predicates.isEmpty()) {
            return true;
         } else if (this.predicates.size() != 1) {
            List<ItemPredicate> list = new ObjectArrayList<>(this.predicates);
            int i = pInventory.getContainerSize();

            for(int j = 0; j < i; ++j) {
               if (list.isEmpty()) {
                  return true;
               }

               ItemStack itemstack = pInventory.getItem(j);
               if (!itemstack.isEmpty()) {
                  list.removeIf((p_43194_) -> {
                     return p_43194_.matches(itemstack);
                  });
               }
            }

            return list.isEmpty();
         } else {
            return !pStack.isEmpty() && this.predicates.get(0).matches(pStack);
         }
      }
   }
}