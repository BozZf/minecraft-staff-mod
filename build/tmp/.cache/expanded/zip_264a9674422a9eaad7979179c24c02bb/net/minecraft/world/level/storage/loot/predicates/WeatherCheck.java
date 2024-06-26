package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.storage.loot.LootContext;

public record WeatherCheck(Optional<Boolean> isRaining, Optional<Boolean> isThundering) implements LootItemCondition {
   public static final Codec<WeatherCheck> CODEC = RecordCodecBuilder.create((p_298666_) -> {
      return p_298666_.group(ExtraCodecs.strictOptionalField(Codec.BOOL, "raining").forGetter(WeatherCheck::isRaining), ExtraCodecs.strictOptionalField(Codec.BOOL, "thundering").forGetter(WeatherCheck::isThundering)).apply(p_298666_, WeatherCheck::new);
   });

   public LootItemConditionType getType() {
      return LootItemConditions.WEATHER_CHECK;
   }

   public boolean test(LootContext pContext) {
      ServerLevel serverlevel = pContext.getLevel();
      if (this.isRaining.isPresent() && this.isRaining.get() != serverlevel.isRaining()) {
         return false;
      } else {
         return !this.isThundering.isPresent() || this.isThundering.get() == serverlevel.isThundering();
      }
   }

   public static WeatherCheck.Builder weather() {
      return new WeatherCheck.Builder();
   }

   public static class Builder implements LootItemCondition.Builder {
      private Optional<Boolean> isRaining = Optional.empty();
      private Optional<Boolean> isThundering = Optional.empty();

      public WeatherCheck.Builder setRaining(boolean pIsRaining) {
         this.isRaining = Optional.of(pIsRaining);
         return this;
      }

      public WeatherCheck.Builder setThundering(boolean pIsThundering) {
         this.isThundering = Optional.of(pIsThundering);
         return this;
      }

      public WeatherCheck build() {
         return new WeatherCheck(this.isRaining, this.isThundering);
      }
   }
}