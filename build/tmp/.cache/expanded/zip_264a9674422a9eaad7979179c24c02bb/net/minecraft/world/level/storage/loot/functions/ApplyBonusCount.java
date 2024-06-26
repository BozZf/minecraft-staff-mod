package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * LootItemFunction that modifies the stack's count based on an enchantment level on the {@linkplain
 * LootContextParams#TOOL tool} using various formulas.
 */
public class ApplyBonusCount extends LootItemConditionalFunction {
   private static final Map<ResourceLocation, ApplyBonusCount.FormulaType> FORMULAS = Stream.of(ApplyBonusCount.BinomialWithBonusCount.TYPE, ApplyBonusCount.OreDrops.TYPE, ApplyBonusCount.UniformBonusCount.TYPE).collect(Collectors.toMap(ApplyBonusCount.FormulaType::id, Function.identity()));
   static final Codec<ApplyBonusCount.FormulaType> FORMULA_TYPE_CODEC = ResourceLocation.CODEC.comapFlatMap((p_297073_) -> {
      ApplyBonusCount.FormulaType applybonuscount$formulatype = FORMULAS.get(p_297073_);
      return applybonuscount$formulatype != null ? DataResult.success(applybonuscount$formulatype) : DataResult.error(() -> {
         return "No formula type with id: '" + p_297073_ + "'";
      });
   }, ApplyBonusCount.FormulaType::id);
   private static final MapCodec<ApplyBonusCount.Formula> FORMULA_CODEC = new MapCodec<ApplyBonusCount.Formula>() {
      private static final String TYPE_KEY = "formula";
      private static final String VALUE_KEY = "parameters";

      public <T> Stream<T> keys(DynamicOps<T> p_299919_) {
         return Stream.of(p_299919_.createString("formula"), p_299919_.createString("parameters"));
      }

      public <T> DataResult<ApplyBonusCount.Formula> decode(DynamicOps<T> p_297405_, MapLike<T> p_298221_) {
         T t = p_298221_.get("formula");
         return t == null ? DataResult.error(() -> {
            return "Missing type for formula in: " + p_298221_;
         }) : ApplyBonusCount.FORMULA_TYPE_CODEC.decode(p_297405_, t).flatMap((p_298653_) -> {
            T t1 = Objects.requireNonNullElseGet(p_298221_.get("parameters"), p_297405_::emptyMap);
            return p_298653_.getFirst().codec().decode(p_297405_, t1).map(Pair::getFirst);
         });
      }

      public <T> RecordBuilder<T> encode(ApplyBonusCount.Formula p_299009_, DynamicOps<T> p_298702_, RecordBuilder<T> p_298118_) {
         ApplyBonusCount.FormulaType applybonuscount$formulatype = p_299009_.getType();
         p_298118_.add("formula", ApplyBonusCount.FORMULA_TYPE_CODEC.encodeStart(p_298702_, applybonuscount$formulatype));
         DataResult<T> dataresult = this.encode(applybonuscount$formulatype.codec(), p_299009_, p_298702_);
         if (dataresult.result().isEmpty() || !Objects.equals(dataresult.result().get(), p_298702_.emptyMap())) {
            p_298118_.add("parameters", dataresult);
         }

         return p_298118_;
      }

      private <T, F extends ApplyBonusCount.Formula> DataResult<T> encode(Codec<F> p_298015_, ApplyBonusCount.Formula p_298974_, DynamicOps<T> p_298875_) {
         return p_298015_.encodeStart(p_298875_, (F)p_298974_);
      }
   };
   public static final Codec<ApplyBonusCount> CODEC = RecordCodecBuilder.create((p_297066_) -> {
      return commonFields(p_297066_).and(p_297066_.group(BuiltInRegistries.ENCHANTMENT.holderByNameCodec().fieldOf("enchantment").forGetter((p_297072_) -> {
         return p_297072_.enchantment;
      }), FORMULA_CODEC.forGetter((p_297058_) -> {
         return p_297058_.formula;
      }))).apply(p_297066_, ApplyBonusCount::new);
   });
   private final Holder<Enchantment> enchantment;
   private final ApplyBonusCount.Formula formula;

   private ApplyBonusCount(List<LootItemCondition> p_298095_, Holder<Enchantment> p_298508_, ApplyBonusCount.Formula p_79905_) {
      super(p_298095_);
      this.enchantment = p_298508_;
      this.formula = p_79905_;
   }

   public LootItemFunctionType getType() {
      return LootItemFunctions.APPLY_BONUS;
   }

   /**
    * Get the parameters used by this object.
    */
   public Set<LootContextParam<?>> getReferencedContextParams() {
      return ImmutableSet.of(LootContextParams.TOOL);
   }

   /**
    * Called to perform the actual action of this function, after conditions have been checked.
    */
   public ItemStack run(ItemStack pStack, LootContext pContext) {
      ItemStack itemstack = pContext.getParamOrNull(LootContextParams.TOOL);
      if (itemstack != null) {
         int i = EnchantmentHelper.getItemEnchantmentLevel(this.enchantment.value(), itemstack);
         int j = this.formula.calculateNewCount(pContext.getRandom(), pStack.getCount(), i);
         pStack.setCount(j);
      }

      return pStack;
   }

   public static LootItemConditionalFunction.Builder<?> addBonusBinomialDistributionCount(Enchantment pEnchantment, float pProbability, int pExtraRounds) {
      return simpleBuilder((p_297062_) -> {
         return new ApplyBonusCount(p_297062_, pEnchantment.builtInRegistryHolder(), new ApplyBonusCount.BinomialWithBonusCount(pExtraRounds, pProbability));
      });
   }

   public static LootItemConditionalFunction.Builder<?> addOreBonusCount(Enchantment pEnchantment) {
      return simpleBuilder((p_297064_) -> {
         return new ApplyBonusCount(p_297064_, pEnchantment.builtInRegistryHolder(), new ApplyBonusCount.OreDrops());
      });
   }

   public static LootItemConditionalFunction.Builder<?> addUniformBonusCount(Enchantment pEnchantment) {
      return simpleBuilder((p_297068_) -> {
         return new ApplyBonusCount(p_297068_, pEnchantment.builtInRegistryHolder(), new ApplyBonusCount.UniformBonusCount(1));
      });
   }

   public static LootItemConditionalFunction.Builder<?> addUniformBonusCount(Enchantment pEnchantment, int pBonusMultiplier) {
      return simpleBuilder((p_297071_) -> {
         return new ApplyBonusCount(p_297071_, pEnchantment.builtInRegistryHolder(), new ApplyBonusCount.UniformBonusCount(pBonusMultiplier));
      });
   }

   static record BinomialWithBonusCount(int extraRounds, float probability) implements ApplyBonusCount.Formula {
      private static final Codec<ApplyBonusCount.BinomialWithBonusCount> CODEC = RecordCodecBuilder.create((p_299643_) -> {
         return p_299643_.group(Codec.INT.fieldOf("extra").forGetter(ApplyBonusCount.BinomialWithBonusCount::extraRounds), Codec.FLOAT.fieldOf("probability").forGetter(ApplyBonusCount.BinomialWithBonusCount::probability)).apply(p_299643_, ApplyBonusCount.BinomialWithBonusCount::new);
      });
      public static final ApplyBonusCount.FormulaType TYPE = new ApplyBonusCount.FormulaType(new ResourceLocation("binomial_with_bonus_count"), CODEC);

      public int calculateNewCount(RandomSource pRandom, int pOriginalCount, int pEnchantmentLevel) {
         for(int i = 0; i < pEnchantmentLevel + this.extraRounds; ++i) {
            if (pRandom.nextFloat() < this.probability) {
               ++pOriginalCount;
            }
         }

         return pOriginalCount;
      }

      public ApplyBonusCount.FormulaType getType() {
         return TYPE;
      }
   }

   interface Formula {
      int calculateNewCount(RandomSource pRandom, int pOriginalCount, int pEnchantmentLevel);

      ApplyBonusCount.FormulaType getType();
   }

   static record FormulaType(ResourceLocation id, Codec<? extends ApplyBonusCount.Formula> codec) {
   }

   static record OreDrops() implements ApplyBonusCount.Formula {
      public static final Codec<ApplyBonusCount.OreDrops> CODEC = Codec.unit(ApplyBonusCount.OreDrops::new);
      public static final ApplyBonusCount.FormulaType TYPE = new ApplyBonusCount.FormulaType(new ResourceLocation("ore_drops"), CODEC);

      public int calculateNewCount(RandomSource pRandom, int pOriginalCount, int pEnchantmentLevel) {
         if (pEnchantmentLevel > 0) {
            int i = pRandom.nextInt(pEnchantmentLevel + 2) - 1;
            if (i < 0) {
               i = 0;
            }

            return pOriginalCount * (i + 1);
         } else {
            return pOriginalCount;
         }
      }

      public ApplyBonusCount.FormulaType getType() {
         return TYPE;
      }
   }

   static record UniformBonusCount(int bonusMultiplier) implements ApplyBonusCount.Formula {
      public static final Codec<ApplyBonusCount.UniformBonusCount> CODEC = RecordCodecBuilder.create((p_297464_) -> {
         return p_297464_.group(Codec.INT.fieldOf("bonusMultiplier").forGetter(ApplyBonusCount.UniformBonusCount::bonusMultiplier)).apply(p_297464_, ApplyBonusCount.UniformBonusCount::new);
      });
      public static final ApplyBonusCount.FormulaType TYPE = new ApplyBonusCount.FormulaType(new ResourceLocation("uniform_bonus_count"), CODEC);

      public int calculateNewCount(RandomSource pRandom, int pOriginalCount, int pEnchantmentLevel) {
         return pOriginalCount + pRandom.nextInt(this.bonusMultiplier * pEnchantmentLevel + 1);
      }

      public ApplyBonusCount.FormulaType getType() {
         return TYPE;
      }
   }
}