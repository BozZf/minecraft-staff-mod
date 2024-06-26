package net.minecraft.client.gui.layouts;

import java.util.function.Consumer;
import net.minecraft.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LinearLayout implements Layout {
   private final GridLayout wrapped;
   private final LinearLayout.Orientation orientation;
   private int nextChildIndex = 0;

   private LinearLayout(LinearLayout.Orientation pOrientation) {
      this(0, 0, pOrientation);
   }

   public LinearLayout(int pWidth, int pHeight, LinearLayout.Orientation pOrientation) {
      this.wrapped = new GridLayout(pWidth, pHeight);
      this.orientation = pOrientation;
   }

   public LinearLayout spacing(int pSpacing) {
      this.orientation.setSpacing(this.wrapped, pSpacing);
      return this;
   }

   public LayoutSettings newCellSettings() {
      return this.wrapped.newCellSettings();
   }

   public LayoutSettings defaultCellSetting() {
      return this.wrapped.defaultCellSetting();
   }

   public <T extends LayoutElement> T addChild(T pChild, LayoutSettings pLayoutSettings) {
      return this.orientation.addChild(this.wrapped, pChild, this.nextChildIndex++, pLayoutSettings);
   }

   public <T extends LayoutElement> T addChild(T pChild) {
      return this.addChild(pChild, this.newCellSettings());
   }

   public <T extends LayoutElement> T addChild(T pChild, Consumer<LayoutSettings> pLayoutSettingsFactory) {
      return this.orientation.addChild(this.wrapped, pChild, this.nextChildIndex++, Util.make(this.newCellSettings(), pLayoutSettingsFactory));
   }

   public void visitChildren(Consumer<LayoutElement> pVisitor) {
      this.wrapped.visitChildren(pVisitor);
   }

   public void arrangeElements() {
      this.wrapped.arrangeElements();
   }

   public int getWidth() {
      return this.wrapped.getWidth();
   }

   public int getHeight() {
      return this.wrapped.getHeight();
   }

   public void setX(int pX) {
      this.wrapped.setX(pX);
   }

   public void setY(int pY) {
      this.wrapped.setY(pY);
   }

   public int getX() {
      return this.wrapped.getX();
   }

   public int getY() {
      return this.wrapped.getY();
   }

   public static LinearLayout vertical() {
      return new LinearLayout(LinearLayout.Orientation.VERTICAL);
   }

   public static LinearLayout horizontal() {
      return new LinearLayout(LinearLayout.Orientation.HORIZONTAL);
   }

   @OnlyIn(Dist.CLIENT)
   public static enum Orientation {
      HORIZONTAL,
      VERTICAL;

      void setSpacing(GridLayout pLayout, int pSpacing) {
         switch (this) {
            case HORIZONTAL:
               pLayout.columnSpacing(pSpacing);
               break;
            case VERTICAL:
               pLayout.rowSpacing(pSpacing);
         }

      }

      public <T extends LayoutElement> T addChild(GridLayout pLayout, T pElement, int pIndex, LayoutSettings pLayoutSettings) {
         LayoutElement layoutelement;
         switch (this) {
            case HORIZONTAL:
               layoutelement = pLayout.addChild(pElement, 0, pIndex, pLayoutSettings);
               break;
            case VERTICAL:
               layoutelement = pLayout.addChild(pElement, pIndex, 0, pLayoutSettings);
               break;
            default:
               throw new IncompatibleClassChangeError();
         }

         return (T)layoutelement;
      }
   }
}