package net.minecraft.util.valueproviders;

import com.mojang.serialization.Codec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;

public class ConstantFloat extends FloatProvider {
   public static final ConstantFloat ZERO = new ConstantFloat(0.0F);
   public static final Codec<ConstantFloat> CODEC = ExtraCodecs.withAlternative(Codec.FLOAT, Codec.FLOAT.fieldOf("value").codec()).xmap(ConstantFloat::new, ConstantFloat::getValue);
   private final float value;

   public static ConstantFloat of(float pValue) {
      return pValue == 0.0F ? ZERO : new ConstantFloat(pValue);
   }

   private ConstantFloat(float p_146456_) {
      this.value = p_146456_;
   }

   public float getValue() {
      return this.value;
   }

   public float sample(RandomSource pRandom) {
      return this.value;
   }

   public float getMinValue() {
      return this.value;
   }

   public float getMaxValue() {
      return this.value + 1.0F;
   }

   public FloatProviderType<?> getType() {
      return FloatProviderType.CONSTANT;
   }

   public String toString() {
      return Float.toString(this.value);
   }
}