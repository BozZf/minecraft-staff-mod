package net.minecraft.world.item.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ShapelessRecipe implements CraftingRecipe {
   final String group;
   final CraftingBookCategory category;
   final ItemStack result;
   final NonNullList<Ingredient> ingredients;
   private final boolean isSimple;

   public ShapelessRecipe(String pGroup, CraftingBookCategory pCategory, ItemStack pResult, NonNullList<Ingredient> pIngredients) {
      this.group = pGroup;
      this.category = pCategory;
      this.result = pResult;
      this.ingredients = pIngredients;
      this.isSimple = pIngredients.stream().allMatch(Ingredient::isSimple);
   }

   public RecipeSerializer<?> getSerializer() {
      return RecipeSerializer.SHAPELESS_RECIPE;
   }

   /**
    * Recipes with equal group are combined into one button in the recipe book
    */
   public String getGroup() {
      return this.group;
   }

   public CraftingBookCategory category() {
      return this.category;
   }

   public ItemStack getResultItem(RegistryAccess pRegistryAccess) {
      return this.result;
   }

   public NonNullList<Ingredient> getIngredients() {
      return this.ingredients;
   }

   /**
    * Used to check if a recipe matches current crafting inventory
    */
   public boolean matches(CraftingContainer pInv, Level pLevel) {
      StackedContents stackedcontents = new StackedContents();
      java.util.List<ItemStack> inputs = new java.util.ArrayList<>();
      int i = 0;

      for(int j = 0; j < pInv.getContainerSize(); ++j) {
         ItemStack itemstack = pInv.getItem(j);
         if (!itemstack.isEmpty()) {
            ++i;
            if (isSimple)
            stackedcontents.accountStack(itemstack, 1);
            else inputs.add(itemstack);
         }
      }

      return i == this.ingredients.size() && (isSimple ? stackedcontents.canCraft(this, (IntList)null) : net.minecraftforge.common.util.RecipeMatcher.findMatches(inputs,  this.ingredients) != null);
   }

   public ItemStack assemble(CraftingContainer pContainer, RegistryAccess pRegistryAccess) {
      return this.result.copy();
   }

   /**
    * Used to determine if this recipe can fit in a grid of the given width/height
    */
   public boolean canCraftInDimensions(int pWidth, int pHeight) {
      return pWidth * pHeight >= this.ingredients.size();
   }

   public static class Serializer implements RecipeSerializer<ShapelessRecipe> {
      private static final Codec<ShapelessRecipe> CODEC = RecordCodecBuilder.create((p_300970_) -> {
         return p_300970_.group(ExtraCodecs.strictOptionalField(Codec.STRING, "group", "").forGetter((p_299460_) -> {
            return p_299460_.group;
         }), CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter((p_297437_) -> {
            return p_297437_.category;
         }), CraftingRecipeCodecs.ITEMSTACK_OBJECT_CODEC.fieldOf("result").forGetter((p_300770_) -> {
            return p_300770_.result;
         }), Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients").flatXmap((p_297969_) -> {
            Ingredient[] aingredient = p_297969_.stream().filter((p_298915_) -> {
               return !p_298915_.isEmpty();
            }).toArray((p_298774_) -> {
               return new Ingredient[p_298774_];
            });
            if (aingredient.length == 0) {
               return DataResult.error(() -> {
                  return "No ingredients for shapeless recipe";
               });
            } else {
               return aingredient.length > ShapedRecipe.MAX_WIDTH * ShapedRecipe.MAX_HEIGHT ? DataResult.error(() -> {
                  return "Too many ingredients for shapeless recipe";
               }) : DataResult.success(NonNullList.of(Ingredient.EMPTY, aingredient));
            }
         }, DataResult::success).forGetter((p_298509_) -> {
            return p_298509_.ingredients;
         })).apply(p_300970_, ShapelessRecipe::new);
      });

      public Codec<ShapelessRecipe> codec() {
         return CODEC;
      }

      public ShapelessRecipe fromNetwork(FriendlyByteBuf pBuffer) {
         String s = pBuffer.readUtf();
         CraftingBookCategory craftingbookcategory = pBuffer.readEnum(CraftingBookCategory.class);
         int i = pBuffer.readVarInt();
         NonNullList<Ingredient> nonnulllist = NonNullList.withSize(i, Ingredient.EMPTY);

         for(int j = 0; j < nonnulllist.size(); ++j) {
            nonnulllist.set(j, Ingredient.fromNetwork(pBuffer));
         }

         ItemStack itemstack = pBuffer.readItem();
         return new ShapelessRecipe(s, craftingbookcategory, itemstack, nonnulllist);
      }

      public void toNetwork(FriendlyByteBuf pBuffer, ShapelessRecipe pRecipe) {
         pBuffer.writeUtf(pRecipe.group);
         pBuffer.writeEnum(pRecipe.category);
         pBuffer.writeVarInt(pRecipe.ingredients.size());

         for(Ingredient ingredient : pRecipe.ingredients) {
            ingredient.toNetwork(pBuffer);
         }

         pBuffer.writeItem(pRecipe.result);
      }
   }
}
