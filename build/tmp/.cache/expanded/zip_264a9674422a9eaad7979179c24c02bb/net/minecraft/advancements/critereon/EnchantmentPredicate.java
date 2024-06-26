package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.enchantment.Enchantment;

public record EnchantmentPredicate(Optional<Holder<Enchantment>> enchantment, MinMaxBounds.Ints level) {
   public static final Codec<EnchantmentPredicate> CODEC = RecordCodecBuilder.create((p_296116_) -> {
      return p_296116_.group(ExtraCodecs.strictOptionalField(BuiltInRegistries.ENCHANTMENT.holderByNameCodec(), "enchantment").forGetter(EnchantmentPredicate::enchantment), ExtraCodecs.strictOptionalField(MinMaxBounds.Ints.CODEC, "levels", MinMaxBounds.Ints.ANY).forGetter(EnchantmentPredicate::level)).apply(p_296116_, EnchantmentPredicate::new);
   });

   public EnchantmentPredicate(Enchantment pEnchantment, MinMaxBounds.Ints pLevel) {
      this(Optional.of(pEnchantment.builtInRegistryHolder()), pLevel);
   }

   public boolean containedIn(Map<Enchantment, Integer> pEnchantments) {
      if (this.enchantment.isPresent()) {
         Enchantment enchantment = this.enchantment.get().value();
         if (!pEnchantments.containsKey(enchantment)) {
            return false;
         }

         int i = pEnchantments.get(enchantment);
         if (this.level != MinMaxBounds.Ints.ANY && !this.level.matches(i)) {
            return false;
         }
      } else if (this.level != MinMaxBounds.Ints.ANY) {
         for(Integer integer : pEnchantments.values()) {
            if (this.level.matches(integer)) {
               return true;
            }
         }

         return false;
      }

      return true;
   }
}