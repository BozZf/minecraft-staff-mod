package net.minecraft.world.level.storage.loot.providers.score;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;

public record FixedScoreboardNameProvider(String name) implements ScoreboardNameProvider {
   public static final Codec<FixedScoreboardNameProvider> CODEC = RecordCodecBuilder.create((p_300953_) -> {
      return p_300953_.group(Codec.STRING.fieldOf("name").forGetter(FixedScoreboardNameProvider::name)).apply(p_300953_, FixedScoreboardNameProvider::new);
   });

   public static ScoreboardNameProvider forName(String pName) {
      return new FixedScoreboardNameProvider(pName);
   }

   public LootScoreProviderType getType() {
      return ScoreboardNameProviders.FIXED;
   }

   /**
    * Get the scoreboard name based on the given loot context.
    */
   @Nullable
   public String getScoreboardName(LootContext pLootContext) {
      return this.name;
   }

   public Set<LootContextParam<?>> getReferencedContextParams() {
      return ImmutableSet.of();
   }
}