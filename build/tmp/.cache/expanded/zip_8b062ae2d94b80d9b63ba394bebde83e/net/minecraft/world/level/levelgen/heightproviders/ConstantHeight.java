package net.minecraft.world.level.levelgen.heightproviders;

import com.mojang.serialization.Codec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.WorldGenerationContext;

public class ConstantHeight extends HeightProvider {
   public static final ConstantHeight ZERO = new ConstantHeight(VerticalAnchor.absolute(0));
   public static final Codec<ConstantHeight> CODEC = ExtraCodecs.withAlternative(VerticalAnchor.CODEC, VerticalAnchor.CODEC.fieldOf("value").codec()).xmap(ConstantHeight::new, ConstantHeight::getValue);
   private final VerticalAnchor value;

   public static ConstantHeight of(VerticalAnchor p_161957_) {
      return new ConstantHeight(p_161957_);
   }

   private ConstantHeight(VerticalAnchor p_161950_) {
      this.value = p_161950_;
   }

   public VerticalAnchor getValue() {
      return this.value;
   }

   public int sample(RandomSource p_226300_, WorldGenerationContext p_226301_) {
      return this.value.resolveY(p_226301_);
   }

   public HeightProviderType<?> getType() {
      return HeightProviderType.CONSTANT;
   }

   public String toString() {
      return this.value.toString();
   }
}