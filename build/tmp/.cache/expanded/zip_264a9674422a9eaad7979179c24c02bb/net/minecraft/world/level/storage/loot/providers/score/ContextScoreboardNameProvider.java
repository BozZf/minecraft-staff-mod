package net.minecraft.world.level.storage.loot.providers.score;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;

public record ContextScoreboardNameProvider(LootContext.EntityTarget target) implements ScoreboardNameProvider {
   public static final Codec<ContextScoreboardNameProvider> CODEC = RecordCodecBuilder.create((p_297529_) -> {
      return p_297529_.group(LootContext.EntityTarget.CODEC.fieldOf("target").forGetter(ContextScoreboardNameProvider::target)).apply(p_297529_, ContextScoreboardNameProvider::new);
   });
   public static final Codec<ContextScoreboardNameProvider> INLINE_CODEC = LootContext.EntityTarget.CODEC.xmap(ContextScoreboardNameProvider::new, ContextScoreboardNameProvider::target);

   public static ScoreboardNameProvider forTarget(LootContext.EntityTarget pTarget) {
      return new ContextScoreboardNameProvider(pTarget);
   }

   public LootScoreProviderType getType() {
      return ScoreboardNameProviders.CONTEXT;
   }

   /**
    * Get the scoreboard name based on the given loot context.
    */
   @Nullable
   public String getScoreboardName(LootContext pLootContext) {
      Entity entity = pLootContext.getParamOrNull(this.target.getParam());
      return entity != null ? entity.getScoreboardName() : null;
   }

   public Set<LootContextParam<?>> getReferencedContextParams() {
      return ImmutableSet.of(this.target.getParam());
   }
}