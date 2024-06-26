package net.minecraft.server;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.advancements.TreeNodePosition;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.storage.loot.LootDataManager;
import org.slf4j.Logger;

public class ServerAdvancementManager extends SimpleJsonResourceReloadListener {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Gson GSON = (new GsonBuilder()).create();
   private Map<ResourceLocation, AdvancementHolder> advancements = Map.of();
   private AdvancementTree tree = new AdvancementTree();
   private final LootDataManager lootData;
   private final net.minecraftforge.common.crafting.conditions.ICondition.IContext context; //Forge: add context

   /** @deprecated Forge: use {@linkplain ServerAdvancementManager#ServerAdvancementManager(LootDataManager, net.minecraftforge.common.crafting.conditions.ICondition.IContext) constructor with context}. */
   @Deprecated
   public ServerAdvancementManager(LootDataManager pLootData) {
      this(pLootData, net.minecraftforge.common.crafting.conditions.ICondition.IContext.EMPTY);
   }

   public ServerAdvancementManager(LootDataManager pLootData, net.minecraftforge.common.crafting.conditions.ICondition.IContext context) {
      super(GSON, "advancements");
      this.lootData = pLootData;
      this.context = context;
   }

   /**
    * Applies the prepared sound event registrations and caches to the sound manager.
    * @param pObject The prepared sound event registrations and caches
    * @param pResourceManager The resource manager
    * @param pProfiler The profiler
    */
   protected void apply(Map<ResourceLocation, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
      ImmutableMap.Builder<ResourceLocation, AdvancementHolder> builder = ImmutableMap.builder();
      pObject.forEach((p_296449_, p_296450_) -> {
         try {
            JsonObject jsonobject = GsonHelper.convertToJsonObject(p_296450_, "advancement");
            Advancement advancement = Advancement.fromJson(jsonobject, new DeserializationContext(p_296449_, this.lootData), this.context);
            if (advancement == null) {
                LOGGER.debug("Skipping loading advancement {} as its conditions were not met", p_296449_);
                return;
            }
            builder.put(p_296449_, new AdvancementHolder(p_296449_, advancement));
         } catch (Exception exception) {
            LOGGER.error("Parsing error loading custom advancement {}: {}", p_296449_, exception.getMessage());
         }

      });
      this.advancements = builder.buildOrThrow();
      AdvancementTree advancementtree = new AdvancementTree();
      advancementtree.addAll(this.advancements.values());

      for(AdvancementNode advancementnode : advancementtree.roots()) {
         if (advancementnode.holder().value().display().isPresent()) {
            TreeNodePosition.run(advancementnode);
         }
      }

      this.tree = advancementtree;
   }

   @Nullable
   public AdvancementHolder get(ResourceLocation p_299615_) {
      return this.advancements.get(p_299615_);
   }

   public AdvancementTree tree() {
      return this.tree;
   }

   public Collection<AdvancementHolder> getAllAdvancements() {
      return this.advancements.values();
   }
}
