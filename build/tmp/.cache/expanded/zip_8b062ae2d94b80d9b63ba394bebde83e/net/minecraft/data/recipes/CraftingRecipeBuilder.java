package net.minecraft.data.recipes;

import com.google.gson.JsonObject;
import net.minecraft.world.item.crafting.CraftingBookCategory;

public abstract class CraftingRecipeBuilder {
   protected static CraftingBookCategory determineBookCategory(RecipeCategory p_250736_) {
      CraftingBookCategory craftingbookcategory;
      switch (p_250736_) {
         case BUILDING_BLOCKS:
            craftingbookcategory = CraftingBookCategory.BUILDING;
            break;
         case TOOLS:
         case COMBAT:
            craftingbookcategory = CraftingBookCategory.EQUIPMENT;
            break;
         case REDSTONE:
            craftingbookcategory = CraftingBookCategory.REDSTONE;
            break;
         default:
            craftingbookcategory = CraftingBookCategory.MISC;
      }

      return craftingbookcategory;
   }

   protected abstract static class CraftingResult implements FinishedRecipe {
      private final CraftingBookCategory category;

      protected CraftingResult(CraftingBookCategory p_250313_) {
         this.category = p_250313_;
      }

      private net.minecraftforge.common.crafting.conditions.ICondition condition;
      public <R extends CraftingResult> R withCondition(net.minecraftforge.common.crafting.conditions.ICondition condition) {
         this.condition = condition;
         return (R)this;
      }

      public void serializeRecipeData(JsonObject p_250456_) {
         p_250456_.addProperty("category", this.category.getSerializedName());
         net.minecraftforge.common.ForgeHooks.writeCondition(this.condition, p_250456_);
      }
   }
}
