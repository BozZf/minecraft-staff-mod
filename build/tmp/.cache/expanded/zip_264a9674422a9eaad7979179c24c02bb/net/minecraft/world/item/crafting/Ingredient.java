package net.minecraft.world.item.crafting;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntComparators;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public class Ingredient implements Predicate<ItemStack> {
   //Because Mojang caches things... we need to invalidate them.. so... here we go..
   private static final java.util.concurrent.atomic.AtomicInteger INVALIDATION_COUNTER = new java.util.concurrent.atomic.AtomicInteger();
   public static void invalidateAll() {
      INVALIDATION_COUNTER.incrementAndGet();
   }

   public static final Ingredient EMPTY = new Ingredient(Stream.empty());
   private final Ingredient.Value[] values;
   @Nullable
   private ItemStack[] itemStacks;
   @Nullable
   private IntList stackingIds;
   /** The vanilla codec that doesn't support custom Ingredient types. */
   public static final Codec<Ingredient> VANILLA_CODEC = codec(true);
   public static final Codec<Ingredient> CODEC = net.minecraftforge.common.ForgeHooks.enhanceIngredientCodec(VANILLA_CODEC);
   private static final Codec<Ingredient> VANILLA_CODEC_NONEMPTY = codec(false);
   public static final Codec<Ingredient> CODEC_NONEMPTY = net.minecraftforge.common.ForgeHooks.enhanceIngredientCodec(VANILLA_CODEC_NONEMPTY);
   private int invalidationCounter;

   protected Ingredient(Stream<? extends Ingredient.Value> pValues) {
      this.values = pValues.toArray((p_43933_) -> {
         return new Ingredient.Value[p_43933_];
      });
   }

   private Ingredient(Ingredient.Value[] p_301101_) {
      this.values = p_301101_;
   }

   public ItemStack[] getItems() {
      if (this.itemStacks == null || checkInvalidation()) {
         this.itemStacks = Arrays.stream(this.values).flatMap((p_43916_) -> {
            return p_43916_.getItems().stream();
         }).distinct().toArray((p_43910_) -> {
            return new ItemStack[p_43910_];
         });
      }

      return this.itemStacks;
   }

   public boolean test(@Nullable ItemStack pStack) {
      if (pStack == null) {
         return false;
      } else if (this.isEmpty()) {
         return pStack.isEmpty();
      } else {
         for(ItemStack itemstack : this.getItems()) {
            if (itemstack.is(pStack.getItem())) {
               return true;
            }
         }

         return false;
      }
   }

   public IntList getStackingIds() {
      if (this.stackingIds == null || checkInvalidation()) {
         this.markValid();
         ItemStack[] aitemstack = this.getItems();
         this.stackingIds = new IntArrayList(aitemstack.length);

         for(ItemStack itemstack : aitemstack) {
            this.stackingIds.add(StackedContents.getStackingIndex(itemstack));
         }

         this.stackingIds.sort(IntComparators.NATURAL_COMPARATOR);
      }

      return this.stackingIds;
   }

   public final void toNetwork(FriendlyByteBuf pBuffer) {
      if (net.minecraftforge.common.ForgeHooks.ingredientToNetwork(pBuffer, this))
      pBuffer.writeCollection(Arrays.asList(this.getItems()), FriendlyByteBuf::writeItem);
   }

   public JsonElement toJson(boolean pAllowEmpty) {
      Codec<Ingredient> codec = pAllowEmpty ? CODEC : CODEC_NONEMPTY;
      return Util.getOrThrow(codec.encodeStart(JsonOps.INSTANCE, this), IllegalStateException::new);
   }

   public boolean isEmpty() {
      return this.values.length == 0;
   }

   public boolean equals(Object pOther) {
      if (pOther instanceof Ingredient ingredient) {
         return Arrays.equals((Object[])this.values, (Object[])ingredient.values);
      } else {
         return false;
      }
   }

   public final boolean checkInvalidation() {
      int currentInvalidationCounter = INVALIDATION_COUNTER.get();
      if (this.invalidationCounter != currentInvalidationCounter) {
         invalidate();
         return true;
      }
      return false;
   }

   protected final void markValid() {
      this.invalidationCounter = INVALIDATION_COUNTER.get();
   }

   protected void invalidate() {
      this.itemStacks = null;
      this.stackingIds = null;
   }

   public boolean isSimple() {
      return true;
   }

   private final boolean isVanilla = this.getClass() == Ingredient.class;
   public final boolean isVanilla() {
      return isVanilla;
   }

   public net.minecraftforge.common.crafting.ingredients.IIngredientSerializer<? extends Ingredient> serializer() {
      if (!isVanilla()) throw new IllegalStateException("Modders must implement Ingredient.codec in their custom Ingredients: " + getClass());
      return net.minecraftforge.common.crafting.ingredients.IIngredientSerializer.VANILLA;
   }

   public static Ingredient fromValues(Stream<? extends Ingredient.Value> pStream) {
      Ingredient ingredient = new Ingredient(pStream);
      return ingredient.isEmpty() ? EMPTY : ingredient;
   }

   public static Ingredient of() {
      return EMPTY;
   }

   public static Ingredient of(ItemLike... pItems) {
      return of(Arrays.stream(pItems).map(ItemStack::new));
   }

   public static Ingredient of(ItemStack... pStacks) {
      return of(Arrays.stream(pStacks));
   }

   public static Ingredient of(Stream<ItemStack> pStacks) {
      return fromValues(pStacks.filter((p_43944_) -> {
         return !p_43944_.isEmpty();
      }).map(Ingredient.ItemValue::new));
   }

   public static Ingredient of(TagKey<Item> pTag) {
      return fromValues(Stream.of(new Ingredient.TagValue(pTag)));
   }

   public static Ingredient fromNetwork(FriendlyByteBuf pBuffer) {
      var size = pBuffer.readVarInt();
      if (size == -1) return net.minecraftforge.common.ForgeHooks.ingredientFromNetwork(pBuffer);
      return fromValues(Stream.generate(() -> new Ingredient.ItemValue(pBuffer.readItem())).limit(size));
   }

   private static Codec<Ingredient> codec(boolean pAllowEmpty) {
      Codec<Ingredient.Value[]> codec = Codec.list(Ingredient.Value.CODEC).comapFlatMap((p_296902_) -> {
         return !pAllowEmpty && p_296902_.size() < 1 ? DataResult.error(() -> {
            return "Item array cannot be empty, at least one item must be defined";
         }) : DataResult.success(p_296902_.toArray(new Ingredient.Value[0]));
      }, List::of);
      return ExtraCodecs.either(codec, Ingredient.Value.CODEC).flatComapMap((p_296900_) -> {
         return p_296900_.map(Ingredient::new, (p_296903_) -> {
            return new Ingredient(new Ingredient.Value[]{p_296903_});
         });
      }, (p_296899_) -> {
         if (p_296899_.values.length == 1) {
            return DataResult.success(Either.right(p_296899_.values[0]));
         } else {
            return p_296899_.values.length == 0 && !pAllowEmpty ? DataResult.error(() -> {
               return "Item array cannot be empty, at least one item must be defined";
            }) : DataResult.success(Either.left(p_296899_.values));
         }
      });
   }

   public static record ItemValue(ItemStack item) implements Ingredient.Value {
      static final Codec<Ingredient.ItemValue> CODEC = RecordCodecBuilder.create((p_300421_) -> {
         return p_300421_.group(CraftingRecipeCodecs.ITEMSTACK_NONAIR_CODEC.fieldOf("item").forGetter((p_299657_) -> {
            return p_299657_.item;
         })).apply(p_300421_, Ingredient.ItemValue::new);
      });

      public boolean equals(Object pOther) {
         if (!(pOther instanceof Ingredient.ItemValue ingredient$itemvalue)) {
            return false;
         } else {
            return ingredient$itemvalue.item.getItem().equals(this.item.getItem()) && ingredient$itemvalue.item.getCount() == this.item.getCount();
         }
      }

      public Collection<ItemStack> getItems() {
         return Collections.singleton(this.item);
      }
   }

   public static record TagValue(TagKey<Item> tag) implements Ingredient.Value {
      static final Codec<Ingredient.TagValue> CODEC = RecordCodecBuilder.create((p_300241_) -> {
         return p_300241_.group(TagKey.codec(Registries.ITEM).fieldOf("tag").forGetter((p_301340_) -> {
            return p_301340_.tag;
         })).apply(p_300241_, Ingredient.TagValue::new);
      });

      public boolean equals(Object pOther) {
         if (pOther instanceof Ingredient.TagValue ingredient$tagvalue) {
            return ingredient$tagvalue.tag.location().equals(this.tag.location());
         } else {
            return false;
         }
      }

      public Collection<ItemStack> getItems() {
         List<ItemStack> list = Lists.newArrayList();

         for(Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(this.tag)) {
            list.add(new ItemStack(holder));
         }

         if (list.size() == 0) {
            list.add(new ItemStack(net.minecraft.world.level.block.Blocks.BARRIER).setHoverName(net.minecraft.network.chat.Component.literal("Empty Tag: " + this.tag.location())));
         }
         return list;
      }
   }

   public interface Value {
      Codec<Ingredient.Value> CODEC = ExtraCodecs.xor(Ingredient.ItemValue.CODEC, Ingredient.TagValue.CODEC).xmap((p_300070_) -> {
         return p_300070_.map((p_301348_) -> {
            return p_301348_;
         }, (p_298354_) -> {
            return p_298354_;
         });
      }, (p_299608_) -> {
         if (p_299608_ instanceof Ingredient.TagValue ingredient$tagvalue) {
            return Either.right(ingredient$tagvalue);
         } else if (p_299608_ instanceof Ingredient.ItemValue ingredient$itemvalue) {
            return Either.left(ingredient$itemvalue);
         } else {
            throw new UnsupportedOperationException("This is neither an item value nor a tag value.");
         }
      });

      Collection<ItemStack> getItems();
   }
}
