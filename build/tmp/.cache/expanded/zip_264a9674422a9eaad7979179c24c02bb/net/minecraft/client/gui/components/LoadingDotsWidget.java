package net.minecraft.client.gui.components;

import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.LoadingDotsText;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LoadingDotsWidget extends AbstractWidget {
   private final Font font;

   public LoadingDotsWidget(Font pFont, Component pMessage) {
      super(0, 0, pFont.width(pMessage), 9 * 3, pMessage);
      this.font = pFont;
   }

   protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
      int i = this.getX() + this.getWidth() / 2;
      int j = this.getY() + this.getHeight() / 2;
      Component component = this.getMessage();
      pGuiGraphics.drawString(this.font, component, i - this.font.width(component) / 2, j - 9, -1, false);
      String s = LoadingDotsText.get(Util.getMillis());
      pGuiGraphics.drawString(this.font, s, i - this.font.width(s) / 2, j + 9, -8355712, false);
   }

   protected void updateWidgetNarration(NarrationElementOutput pNarrationElementOutput) {
      pNarrationElementOutput.add(NarratedElementType.TITLE, this.getMessage());
   }
}