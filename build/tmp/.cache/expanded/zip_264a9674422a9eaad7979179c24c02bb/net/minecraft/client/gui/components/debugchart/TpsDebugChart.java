package net.minecraft.client.gui.components.debugchart;

import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.SampleLogger;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TpsDebugChart extends AbstractDebugChart {
   private static final int RED = -65536;
   private static final int YELLOW = -256;
   private static final int GREEN = -16711936;
   private static final int CHART_TOP_VALUE = 50;

   public TpsDebugChart(Font pFont, SampleLogger pLogger) {
      super(pFont, pLogger);
   }

   protected void renderAdditionalLinesAndLabels(GuiGraphics pGuiGraphics, int pX, int pWidth, int pHeight) {
      this.drawStringWithShade(pGuiGraphics, "20 TPS", pX + 1, pHeight - 60 + 1);
   }

   protected String toDisplayString(double pValue) {
      return String.format(Locale.ROOT, "%d ms", (int)Math.round(toMilliseconds(pValue)));
   }

   protected int getSampleHeight(double pValue) {
      return (int)Math.round(toMilliseconds(pValue) * 60.0D / 50.0D);
   }

   protected int getSampleColor(long pValue) {
      return this.getSampleColor(toMilliseconds((double)pValue), 0.0D, -16711936, 25.0D, -256, 50.0D, -65536);
   }

   private static double toMilliseconds(double pValue) {
      return pValue / 1000000.0D;
   }
}