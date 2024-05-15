package net.minecraft.advancements;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public enum FrameType {
   TASK("task", ChatFormatting.GREEN),
   CHALLENGE("challenge", ChatFormatting.DARK_PURPLE),
   GOAL("goal", ChatFormatting.GREEN);

   private final String name;
   private final ChatFormatting chatColor;
   private final Component displayName;

   private FrameType(String pName, ChatFormatting pChatColor) {
      this.name = pName;
      this.chatColor = pChatColor;
      this.displayName = Component.translatable("advancements.toast." + pName);
   }

   public String getName() {
      return this.name;
   }

   public static FrameType byName(String pName) {
      for(FrameType frametype : values()) {
         if (frametype.name.equals(pName)) {
            return frametype;
         }
      }

      throw new IllegalArgumentException("Unknown frame type '" + pName + "'");
   }

   public ChatFormatting getChatColor() {
      return this.chatColor;
   }

   public Component getDisplayName() {
      return this.displayName;
   }
}