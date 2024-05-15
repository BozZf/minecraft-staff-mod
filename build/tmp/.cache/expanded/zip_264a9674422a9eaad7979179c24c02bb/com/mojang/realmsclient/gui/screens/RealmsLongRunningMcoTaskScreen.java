package com.mojang.realmsclient.gui.screens;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.exception.RealmsDefaultUncaughtExceptionHandler;
import com.mojang.realmsclient.util.task.LongRunningTask;
import java.time.Duration;
import javax.annotation.Nullable;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.LoadingDotsWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.realms.RepeatedNarrator;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsLongRunningMcoTaskScreen extends RealmsScreen {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final RepeatedNarrator REPEATED_NARRATOR = new RepeatedNarrator(Duration.ofSeconds(5L));
   private LongRunningTask task;
   private final Screen lastScreen;
   private volatile Component title = CommonComponents.EMPTY;
   private final LinearLayout layout = LinearLayout.vertical();
   @Nullable
   private LoadingDotsWidget loadingDotsWidget;

   public RealmsLongRunningMcoTaskScreen(Screen pLastScreen, LongRunningTask pTask) {
      super(GameNarrator.NO_TITLE);
      this.lastScreen = pLastScreen;
      this.task = pTask;
      this.setTitle(pTask.getTitle());
      Thread thread = new Thread(pTask, "Realms-long-running-task");
      thread.setUncaughtExceptionHandler(new RealmsDefaultUncaughtExceptionHandler(LOGGER));
      thread.start();
   }

   public void tick() {
      super.tick();
      REPEATED_NARRATOR.narrate(this.minecraft.getNarrator(), this.loadingDotsWidget.getMessage());
   }

   /**
    * Called when a keyboard key is pressed within the GUI element.
    * <p>
    * @return {@code true} if the event is consumed, {@code false} otherwise.
    * @param pKeyCode the key code of the pressed key.
    * @param pScanCode the scan code of the pressed key.
    * @param pModifiers the keyboard modifiers.
    */
   public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      if (pKeyCode == 256) {
         this.cancel();
         return true;
      } else {
         return super.keyPressed(pKeyCode, pScanCode, pModifiers);
      }
   }

   public void init() {
      this.layout.defaultCellSetting().alignHorizontallyCenter();
      this.loadingDotsWidget = new LoadingDotsWidget(this.font, this.title);
      this.layout.addChild(this.loadingDotsWidget, (p_296060_) -> {
         p_296060_.paddingBottom(30);
      });
      this.layout.addChild(Button.builder(CommonComponents.GUI_CANCEL, (p_296059_) -> {
         this.cancel();
      }).build());
      this.layout.visitWidgets((p_296062_) -> {
         AbstractWidget abstractwidget = this.addRenderableWidget(p_296062_);
      });
      this.repositionElements();
   }

   protected void repositionElements() {
      this.layout.arrangeElements();
      FrameLayout.centerInRectangle(this.layout, this.getRectangle());
   }

   protected void cancel() {
      this.task.abortTask();
      this.minecraft.setScreen(this.lastScreen);
   }

   public void setTitle(Component pTitle) {
      if (this.loadingDotsWidget != null) {
         this.loadingDotsWidget.setMessage(pTitle);
      }

      this.title = pTitle;
   }
}