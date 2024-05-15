package net.minecraft.advancements;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public record Advancement(Optional<ResourceLocation> parent, Optional<DisplayInfo> display, AdvancementRewards rewards, Map<String, Criterion<?>> criteria, AdvancementRequirements requirements, boolean sendsTelemetryEvent, Optional<Component> name) {
   public Advancement(Optional<ResourceLocation> pParent, Optional<DisplayInfo> pDisplay, AdvancementRewards pRewards, Map<String, Criterion<?>> pCriteria, AdvancementRequirements pRequirements, boolean pSendsTelemetryEvent) {
      this(pParent, pDisplay, pRewards, Map.copyOf(pCriteria), pRequirements, pSendsTelemetryEvent, pDisplay.map(Advancement::decorateName));
   }

   private static Component decorateName(DisplayInfo p_300038_) {
      Component component = p_300038_.getTitle();
      ChatFormatting chatformatting = p_300038_.getFrame().getChatColor();
      Component component1 = ComponentUtils.mergeStyles(component.copy(), Style.EMPTY.withColor(chatformatting)).append("\n").append(p_300038_.getDescription());
      Component component2 = component.copy().withStyle((p_138316_) -> {
         return p_138316_.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, component1));
      });
      return ComponentUtils.wrapInSquareBrackets(component2).withStyle(chatformatting);
   }

   public static Component name(AdvancementHolder pAdvancement) {
      return pAdvancement.value().name().orElseGet(() -> {
         return Component.literal(pAdvancement.id().toString());
      });
   }

   public JsonObject serializeToJson() {
      JsonObject jsonobject = new JsonObject();
      this.parent.ifPresent((p_296095_) -> {
         jsonobject.addProperty("parent", p_296095_.toString());
      });
      this.display.ifPresent((p_296097_) -> {
         jsonobject.add("display", p_296097_.serializeToJson());
      });
      jsonobject.add("rewards", this.rewards.serializeToJson());
      JsonObject jsonobject1 = new JsonObject();

      for(Map.Entry<String, Criterion<?>> entry : this.criteria.entrySet()) {
         jsonobject1.add(entry.getKey(), entry.getValue().serializeToJson());
      }

      jsonobject.add("criteria", jsonobject1);
      jsonobject.add("requirements", this.requirements.toJson());
      jsonobject.addProperty("sends_telemetry_event", this.sendsTelemetryEvent);
      return jsonobject;
   }

   /** @deprecated Forge: use {@linkplain #fromJson(JsonObject, DeserializationContext, net.minecraftforge.common.crafting.conditions.ICondition.IContext) overload with context}. */
   @Deprecated
   public static Advancement fromJson(JsonObject pJson, DeserializationContext pDeserializationContext) {
       return fromJson(pJson, pDeserializationContext, net.minecraftforge.common.crafting.conditions.ICondition.IContext.EMPTY);
   }

   public static Advancement fromJson(JsonObject pJson, DeserializationContext pDeserializationContext, net.minecraftforge.common.crafting.conditions.ICondition.IContext context) {
      if ((pJson = net.minecraftforge.common.ForgeHooks.readConditionalAdvancement(context, pJson)) == null) return null;
      Optional<ResourceLocation> optional = pJson.has("parent") ? Optional.of(new ResourceLocation(GsonHelper.getAsString(pJson, "parent"))) : Optional.empty();
      Optional<DisplayInfo> optional1 = pJson.has("display") ? Optional.of(DisplayInfo.fromJson(GsonHelper.getAsJsonObject(pJson, "display"))) : Optional.empty();
      AdvancementRewards advancementrewards = pJson.has("rewards") ? AdvancementRewards.deserialize(GsonHelper.getAsJsonObject(pJson, "rewards")) : AdvancementRewards.EMPTY;
      Map<String, Criterion<?>> map = Criterion.criteriaFromJson(GsonHelper.getAsJsonObject(pJson, "criteria"), pDeserializationContext);
      if (map.isEmpty()) {
         throw new JsonSyntaxException("Advancement criteria cannot be empty");
      } else {
         JsonArray jsonarray = GsonHelper.getAsJsonArray(pJson, "requirements", new JsonArray());
         AdvancementRequirements advancementrequirements;
         if (jsonarray.isEmpty()) {
            advancementrequirements = AdvancementRequirements.allOf(map.keySet());
         } else {
            advancementrequirements = AdvancementRequirements.fromJson(jsonarray, map.keySet());
         }

         boolean flag = GsonHelper.getAsBoolean(pJson, "sends_telemetry_event", false);
         return new Advancement(optional, optional1, advancementrewards, map, advancementrequirements, flag);
      }
   }

   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeOptional(this.parent, FriendlyByteBuf::writeResourceLocation);
      pBuffer.writeOptional(this.display, (p_296098_, p_296099_) -> {
         p_296099_.serializeToNetwork(p_296098_);
      });
      this.requirements.write(pBuffer);
      pBuffer.writeBoolean(this.sendsTelemetryEvent);
   }

   public static Advancement read(FriendlyByteBuf pBuffer) {
      return new Advancement(pBuffer.readOptional(FriendlyByteBuf::readResourceLocation), pBuffer.readOptional(DisplayInfo::fromNetwork), AdvancementRewards.EMPTY, Map.of(), new AdvancementRequirements(pBuffer), pBuffer.readBoolean());
   }

   public boolean isRoot() {
      return this.parent.isEmpty();
   }

   public static class Builder {
      private Optional<ResourceLocation> parent = Optional.empty();
      private Optional<DisplayInfo> display = Optional.empty();
      private AdvancementRewards rewards = AdvancementRewards.EMPTY;
      private final ImmutableMap.Builder<String, Criterion<?>> criteria = ImmutableMap.builder();
      private Optional<AdvancementRequirements> requirements = Optional.empty();
      private AdvancementRequirements.Strategy requirementsStrategy = AdvancementRequirements.Strategy.AND;
      private boolean sendsTelemetryEvent;

      public static Advancement.Builder advancement() {
         return (new Advancement.Builder()).sendsTelemetryEvent();
      }

      public static Advancement.Builder recipeAdvancement() {
         return new Advancement.Builder();
      }

      public Advancement.Builder parent(AdvancementHolder pParent) {
         this.parent = Optional.of(pParent.id());
         return this;
      }

      /** @deprecated */
      @Deprecated(
         forRemoval = true
      )
      public Advancement.Builder parent(ResourceLocation pParentId) {
         this.parent = Optional.of(pParentId);
         return this;
      }

      public Advancement.Builder display(ItemStack pStack, Component pTitle, Component pDescription, @Nullable ResourceLocation pBackground, FrameType pFrame, boolean pShowToast, boolean pAnnounceToChat, boolean pHidden) {
         return this.display(new DisplayInfo(pStack, pTitle, pDescription, pBackground, pFrame, pShowToast, pAnnounceToChat, pHidden));
      }

      public Advancement.Builder display(ItemLike pItem, Component pTitle, Component pDescription, @Nullable ResourceLocation pBackground, FrameType pFrame, boolean pShowToast, boolean pAnnounceToChat, boolean pHidden) {
         return this.display(new DisplayInfo(new ItemStack(pItem.asItem()), pTitle, pDescription, pBackground, pFrame, pShowToast, pAnnounceToChat, pHidden));
      }

      public Advancement.Builder display(DisplayInfo pDisplay) {
         this.display = Optional.of(pDisplay);
         return this;
      }

      public Advancement.Builder rewards(AdvancementRewards.Builder pRewardsBuilder) {
         return this.rewards(pRewardsBuilder.build());
      }

      public Advancement.Builder rewards(AdvancementRewards pRewards) {
         this.rewards = pRewards;
         return this;
      }

      public Advancement.Builder addCriterion(String pKey, Criterion<?> pCriterion) {
         this.criteria.put(pKey, pCriterion);
         return this;
      }

      public Advancement.Builder requirements(AdvancementRequirements.Strategy pRequirementsStrategy) {
         this.requirementsStrategy = pRequirementsStrategy;
         return this;
      }

      public Advancement.Builder requirements(AdvancementRequirements pRequirements) {
         this.requirements = Optional.of(pRequirements);
         return this;
      }

      public Advancement.Builder sendsTelemetryEvent() {
         this.sendsTelemetryEvent = true;
         return this;
      }

      public AdvancementHolder build(ResourceLocation pId) {
         Map<String, Criterion<?>> map = this.criteria.buildOrThrow();
         AdvancementRequirements advancementrequirements = this.requirements.orElseGet(() -> {
            return this.requirementsStrategy.create(map.keySet());
         });
         return new AdvancementHolder(pId, new Advancement(this.parent, this.display, this.rewards, map, advancementrequirements, this.sendsTelemetryEvent));
      }

      public AdvancementHolder save(Consumer<AdvancementHolder> pOutput, String pId) {
          return save(pOutput, new ResourceLocation(pId));
      }

      public AdvancementHolder save(Consumer<AdvancementHolder> pOutput, ResourceLocation id) {
         AdvancementHolder advancementholder = this.build(id);
         pOutput.accept(advancementholder);
         return advancementholder;
      }
   }
}
