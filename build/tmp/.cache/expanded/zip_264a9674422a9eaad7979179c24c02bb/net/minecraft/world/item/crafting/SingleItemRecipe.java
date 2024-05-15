package net.minecraft.world.item.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public abstract class SingleItemRecipe implements Recipe<Container> {
   protected final Ingredient ingredient;
   protected final ItemStack result;
   private final RecipeType<?> type;
   private final RecipeSerializer<?> serializer;
   protected final String group;

   public SingleItemRecipe(RecipeType<?> pType, RecipeSerializer<?> pSerializer, String pGroup, Ingredient pIngredient, ItemStack pResult) {
      this.type = pType;
      this.serializer = pSerializer;
      this.group = pGroup;
      this.ingredient = pIngredient;
      this.result = pResult;
   }

   public RecipeType<?> getType() {
      return this.type;
   }

   public RecipeSerializer<?> getSerializer() {
      return this.serializer;
   }

   /**
    * Recipes with equal group are combined into one button in the recipe book
    */
   public String getGroup() {
      return this.group;
   }

   public ItemStack getResultItem(RegistryAccess pRegistryAccess) {
      return this.result;
   }

   public NonNullList<Ingredient> getIngredients() {
      NonNullList<Ingredient> nonnulllist = NonNullList.create();
      nonnulllist.add(this.ingredient);
      return nonnulllist;
   }

   /**
    * Used to determine if this recipe can fit in a grid of the given width/height
    */
   public boolean canCraftInDimensions(int pWidth, int pHeight) {
      return true;
   }

   public ItemStack assemble(Container pContainer, RegistryAccess pRegistryAccess) {
      return this.result.copy();
   }

   public static class Serializer<T extends SingleItemRecipe> implements RecipeSerializer<T> {
      private static final MapCodec<ItemStack> RESULT_CODEC = RecordCodecBuilder.mapCodec((p_301689_) -> {
         return p_301689_.group(BuiltInRegistries.ITEM.byNameCodec().fieldOf("result").forGetter(ItemStack::getItem), Codec.INT.fieldOf("count").forGetter(ItemStack::getCount)).apply(p_301689_, ItemStack::new);
      });
      final SingleItemRecipe.Serializer.SingleItemMaker<T> factory;
      private final Codec<T> codec;

      protected Serializer(SingleItemRecipe.Serializer.SingleItemMaker<T> pFactory) {
         this.factory = pFactory;
         this.codec = RecordCodecBuilder.create((p_300308_) -> {
            return p_300308_.group(ExtraCodecs.strictOptionalField(Codec.STRING, "group", "").forGetter((p_298324_) -> {
               return p_298324_.group;
            }), Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter((p_299566_) -> {
               return p_299566_.ingredient;
            }), RESULT_CODEC.forGetter((p_301692_) -> {
               return p_301692_.result;
            })).apply(p_300308_, pFactory::create);
         });
      }

      public Codec<T> codec() {
         return this.codec;
      }

      public T fromNetwork(FriendlyByteBuf pBuffer) {
         String s = pBuffer.readUtf();
         Ingredient ingredient = Ingredient.fromNetwork(pBuffer);
         ItemStack itemstack = pBuffer.readItem();
         return this.factory.create(s, ingredient, itemstack);
      }

      public void toNetwork(FriendlyByteBuf pBuffer, T pRecipe) {
         pBuffer.writeUtf(pRecipe.group);
         pRecipe.ingredient.toNetwork(pBuffer);
         pBuffer.writeItem(pRecipe.result);
      }

      interface SingleItemMaker<T extends SingleItemRecipe> {
         T create(String pGroup, Ingredient pIngredient, ItemStack pResult);
      }
   }
}