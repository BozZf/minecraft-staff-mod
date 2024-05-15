package com.mojang.realmsclient;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.RateLimiter;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import com.mojang.realmsclient.client.Ping;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.PingResult;
import com.mojang.realmsclient.dto.RealmsNotification;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RegionPingResult;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.RealmsDataFetcher;
import com.mojang.realmsclient.gui.RealmsServerList;
import com.mojang.realmsclient.gui.screens.RealmsConfigureWorldScreen;
import com.mojang.realmsclient.gui.screens.RealmsCreateRealmScreen;
import com.mojang.realmsclient.gui.screens.RealmsGenericErrorScreen;
import com.mojang.realmsclient.gui.screens.RealmsLongConfirmationScreen;
import com.mojang.realmsclient.gui.screens.RealmsLongRunningMcoTaskScreen;
import com.mojang.realmsclient.gui.screens.RealmsPendingInvitesScreen;
import com.mojang.realmsclient.gui.screens.RealmsPopupScreen;
import com.mojang.realmsclient.gui.task.DataFetcher;
import com.mojang.realmsclient.util.RealmsPersistence;
import com.mojang.realmsclient.util.RealmsUtil;
import com.mojang.realmsclient.util.task.GetServerDetailsTask;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.ImageWidget;
import net.minecraft.client.gui.components.LoadingDotsWidget;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsObjectSelectionList;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.CommonLinks;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsMainScreen extends RealmsScreen {
   static final ResourceLocation INFO_SPRITE = new ResourceLocation("icon/info");
   static final ResourceLocation NEW_REALM_SPRITE = new ResourceLocation("icon/new_realm");
   static final ResourceLocation EXPIRED_SPRITE = new ResourceLocation("realm_status/expired");
   static final ResourceLocation EXPIRES_SOON_SPRITE = new ResourceLocation("realm_status/expires_soon");
   static final ResourceLocation OPEN_SPRITE = new ResourceLocation("realm_status/open");
   static final ResourceLocation CLOSED_SPRITE = new ResourceLocation("realm_status/closed");
   private static final ResourceLocation INVITE_SPRITE = new ResourceLocation("icon/invite");
   private static final ResourceLocation NEWS_SPRITE = new ResourceLocation("icon/news");
   static final Logger LOGGER = LogUtils.getLogger();
   private static final ResourceLocation LOGO_LOCATION = new ResourceLocation("textures/gui/title/realms.png");
   private static final ResourceLocation NO_REALMS_LOCATION = new ResourceLocation("textures/gui/realms/no_realms.png");
   private static final Component TITLE = Component.translatable("menu.online");
   private static final Component LOADING_TEXT = Component.translatable("mco.selectServer.loading");
   static final Component SERVER_UNITIALIZED_TEXT = Component.translatable("mco.selectServer.uninitialized").withStyle(ChatFormatting.GREEN);
   static final Component SUBSCRIPTION_EXPIRED_TEXT = Component.translatable("mco.selectServer.expiredList");
   private static final Component SUBSCRIPTION_RENEW_TEXT = Component.translatable("mco.selectServer.expiredRenew");
   static final Component TRIAL_EXPIRED_TEXT = Component.translatable("mco.selectServer.expiredTrial");
   static final Component SELECT_MINIGAME_PREFIX = Component.translatable("mco.selectServer.minigame").append(CommonComponents.SPACE);
   private static final Component PLAY_TEXT = Component.translatable("mco.selectServer.play");
   private static final Component LEAVE_SERVER_TEXT = Component.translatable("mco.selectServer.leave");
   private static final Component CONFIGURE_SERVER_TEXT = Component.translatable("mco.selectServer.configure");
   static final Component SERVER_EXPIRED_TOOLTIP = Component.translatable("mco.selectServer.expired");
   static final Component SERVER_EXPIRES_SOON_TOOLTIP = Component.translatable("mco.selectServer.expires.soon");
   static final Component SERVER_EXPIRES_IN_DAY_TOOLTIP = Component.translatable("mco.selectServer.expires.day");
   static final Component SERVER_OPEN_TOOLTIP = Component.translatable("mco.selectServer.open");
   static final Component SERVER_CLOSED_TOOLTIP = Component.translatable("mco.selectServer.closed");
   static final Component UNITIALIZED_WORLD_NARRATION = Component.translatable("gui.narrate.button", SERVER_UNITIALIZED_TEXT);
   private static final Component NO_REALMS_TEXT = Component.translatable("mco.selectServer.noRealms");
   private static final Tooltip NO_PENDING_INVITES = Tooltip.create(Component.translatable("mco.invites.nopending"));
   private static final Tooltip PENDING_INVITES = Tooltip.create(Component.translatable("mco.invites.pending"));
   private static final int BUTTON_WIDTH = 100;
   private static final int BUTTON_COLUMNS = 3;
   private static final int BUTTON_SPACING = 4;
   private static final int CONTENT_WIDTH = 308;
   private static final int LOGO_WIDTH = 128;
   private static final int LOGO_HEIGHT = 34;
   private static final int LOGO_TEXTURE_WIDTH = 128;
   private static final int LOGO_TEXTURE_HEIGHT = 64;
   private static final int LOGO_PADDING = 5;
   private static final int HEADER_HEIGHT = 44;
   private static final int FOOTER_PADDING = 10;
   private static final int ENTRY_WIDTH = 216;
   private static final int ITEM_HEIGHT = 36;
   private final CompletableFuture<RealmsAvailability.Result> availability = RealmsAvailability.get();
   @Nullable
   private DataFetcher.Subscription dataSubscription;
   private final Set<UUID> handledSeenNotifications = new HashSet<>();
   private static boolean regionsPinged;
   private final RateLimiter inviteNarrationLimiter;
   private final Screen lastScreen;
   private Button playButton;
   private Button backButton;
   private Button renewButton;
   private Button configureButton;
   private Button leaveButton;
   private RealmsMainScreen.RealmSelectionList realmSelectionList;
   private RealmsServerList serverList;
   private volatile boolean trialsAvailable;
   @Nullable
   private volatile String newsLink;
   long lastClickTime;
   private final List<RealmsNotification> notifications = new ArrayList<>();
   private Button addRealmButton;
   private RealmsMainScreen.NotificationButton pendingInvitesButton;
   private RealmsMainScreen.NotificationButton newsButton;
   private RealmsMainScreen.LayoutState activeLayoutState;
   @Nullable
   private HeaderAndFooterLayout layout;

   public RealmsMainScreen(Screen pLastScreen) {
      super(TITLE);
      this.lastScreen = pLastScreen;
      this.inviteNarrationLimiter = RateLimiter.create((double)0.016666668F);
   }

   public void init() {
      this.serverList = new RealmsServerList(this.minecraft);
      this.realmSelectionList = this.addRenderableWidget(new RealmsMainScreen.RealmSelectionList());
      Component component = Component.translatable("mco.invites.title");
      this.pendingInvitesButton = new RealmsMainScreen.NotificationButton(component, INVITE_SPRITE, (p_296029_) -> {
         this.minecraft.setScreen(new RealmsPendingInvitesScreen(this, component));
      });
      Component component1 = Component.translatable("mco.news");
      this.newsButton = new RealmsMainScreen.NotificationButton(component1, NEWS_SPRITE, (p_296035_) -> {
         if (this.newsLink != null) {
            ConfirmLinkScreen.confirmLinkNow(this.newsLink, this, true);
            if (this.newsButton.notificationCount() != 0) {
               RealmsPersistence.RealmsPersistenceData realmspersistence$realmspersistencedata = RealmsPersistence.readFile();
               realmspersistence$realmspersistencedata.hasUnreadNews = false;
               RealmsPersistence.writeFile(realmspersistence$realmspersistencedata);
               this.newsButton.setNotificationCount(0);
            }

         }
      });
      this.newsButton.setTooltip(Tooltip.create(component1));
      this.playButton = Button.builder(PLAY_TEXT, (p_86659_) -> {
         play(this.getSelectedServer(), this);
      }).width(100).build();
      this.configureButton = Button.builder(CONFIGURE_SERVER_TEXT, (p_86672_) -> {
         this.configureClicked(this.getSelectedServer());
      }).width(100).build();
      this.renewButton = Button.builder(SUBSCRIPTION_RENEW_TEXT, (p_86622_) -> {
         this.onRenew(this.getSelectedServer());
      }).width(100).build();
      this.leaveButton = Button.builder(LEAVE_SERVER_TEXT, (p_86679_) -> {
         this.leaveClicked(this.getSelectedServer());
      }).width(100).build();
      this.addRealmButton = Button.builder(Component.translatable("mco.selectServer.purchase"), (p_296032_) -> {
         this.openTrialAvailablePopup();
      }).size(100, 20).build();
      this.backButton = Button.builder(CommonComponents.GUI_BACK, (p_296030_) -> {
         this.minecraft.setScreen(this.lastScreen);
      }).width(100).build();
      this.updateLayout(RealmsMainScreen.LayoutState.LOADING);
      this.updateButtonStates();
      this.availability.thenAcceptAsync((p_296034_) -> {
         Screen screen = p_296034_.createErrorScreen(this.lastScreen);
         if (screen == null) {
            this.dataSubscription = this.initDataFetcher(this.minecraft.realmsDataFetcher());
         } else {
            this.minecraft.setScreen(screen);
         }

      }, this.screenExecutor);
   }

   protected void repositionElements() {
      if (this.layout != null) {
         this.realmSelectionList.updateSize(this.width, this.height, this.layout.getHeaderHeight(), this.height - this.layout.getFooterHeight());
         this.layout.arrangeElements();
      }

   }

   private void updateLayout(RealmsMainScreen.LayoutState pLayoutState) {
      if (this.activeLayoutState != pLayoutState) {
         if (this.layout != null) {
            this.layout.visitWidgets((p_296026_) -> {
               this.removeWidget(p_296026_);
            });
         }

         this.layout = this.createLayout(pLayoutState);
         this.activeLayoutState = pLayoutState;
         this.layout.visitWidgets((p_272289_) -> {
            AbstractWidget abstractwidget = this.addRenderableWidget(p_272289_);
         });
         this.repositionElements();
      }
   }

   private HeaderAndFooterLayout createLayout(RealmsMainScreen.LayoutState pLayoutState) {
      HeaderAndFooterLayout headerandfooterlayout = new HeaderAndFooterLayout(this);
      headerandfooterlayout.setHeaderHeight(44);
      headerandfooterlayout.addToHeader(this.createHeader());
      Layout layout = this.createFooter(pLayoutState);
      layout.arrangeElements();
      headerandfooterlayout.setFooterHeight(layout.getHeight() + 20);
      headerandfooterlayout.addToFooter(layout);
      switch (pLayoutState) {
         case LOADING:
            headerandfooterlayout.addToContents(new LoadingDotsWidget(this.font, LOADING_TEXT));
            break;
         case NO_REALMS:
            headerandfooterlayout.addToContents(this.createNoRealmsContent());
      }

      return headerandfooterlayout;
   }

   private Layout createHeader() {
      int i = 90;
      LinearLayout linearlayout = LinearLayout.horizontal().spacing(4);
      linearlayout.defaultCellSetting().alignVerticallyMiddle();
      linearlayout.addChild(this.pendingInvitesButton);
      linearlayout.addChild(this.newsButton);
      LinearLayout linearlayout1 = LinearLayout.horizontal();
      linearlayout1.defaultCellSetting().alignVerticallyMiddle();
      linearlayout1.addChild(SpacerElement.width(90));
      linearlayout1.addChild(ImageWidget.texture(128, 34, LOGO_LOCATION, 128, 64), LayoutSettings::alignHorizontallyCenter);
      linearlayout1.addChild(new FrameLayout(90, 44)).addChild(linearlayout, LayoutSettings::alignHorizontallyRight);
      return linearlayout1;
   }

   private Layout createFooter(RealmsMainScreen.LayoutState pLayoutState) {
      GridLayout gridlayout = (new GridLayout()).spacing(4);
      GridLayout.RowHelper gridlayout$rowhelper = gridlayout.createRowHelper(3);
      if (pLayoutState == RealmsMainScreen.LayoutState.LIST) {
         gridlayout$rowhelper.addChild(this.playButton);
         gridlayout$rowhelper.addChild(this.configureButton);
         gridlayout$rowhelper.addChild(this.renewButton);
         gridlayout$rowhelper.addChild(this.leaveButton);
      }

      gridlayout$rowhelper.addChild(this.addRealmButton);
      gridlayout$rowhelper.addChild(this.backButton);
      return gridlayout;
   }

   private LinearLayout createNoRealmsContent() {
      LinearLayout linearlayout = LinearLayout.vertical().spacing(10);
      linearlayout.defaultCellSetting().alignHorizontallyCenter();
      linearlayout.addChild(ImageWidget.texture(130, 64, NO_REALMS_LOCATION, 130, 64));
      FocusableTextWidget focusabletextwidget = new FocusableTextWidget(308, NO_REALMS_TEXT, this.font, false);
      linearlayout.addChild(focusabletextwidget);
      return linearlayout;
   }

   void updateButtonStates() {
      RealmsServer realmsserver = this.getSelectedServer();
      this.addRealmButton.active = this.activeLayoutState != RealmsMainScreen.LayoutState.LOADING;
      this.playButton.active = this.shouldPlayButtonBeActive(realmsserver);
      this.renewButton.active = this.shouldRenewButtonBeActive(realmsserver);
      this.leaveButton.active = this.shouldLeaveButtonBeActive(realmsserver);
      this.configureButton.active = this.shouldConfigureButtonBeActive(realmsserver);
   }

   boolean shouldPlayButtonBeActive(@Nullable RealmsServer pRealmsServer) {
      return pRealmsServer != null && !pRealmsServer.expired && pRealmsServer.state == RealmsServer.State.OPEN;
   }

   private boolean shouldRenewButtonBeActive(@Nullable RealmsServer pRealmsServer) {
      return pRealmsServer != null && pRealmsServer.expired && this.isSelfOwnedServer(pRealmsServer);
   }

   private boolean shouldConfigureButtonBeActive(@Nullable RealmsServer pRealmsServer) {
      return pRealmsServer != null && this.isSelfOwnedServer(pRealmsServer);
   }

   private boolean shouldLeaveButtonBeActive(@Nullable RealmsServer pRealmsServer) {
      return pRealmsServer != null && !this.isSelfOwnedServer(pRealmsServer);
   }

   public void tick() {
      super.tick();
      if (this.dataSubscription != null) {
         this.dataSubscription.tick();
      }

   }

   public static void refreshPendingInvites() {
      Minecraft.getInstance().realmsDataFetcher().pendingInvitesTask.reset();
   }

   public void refreshServerList() {
      Minecraft.getInstance().realmsDataFetcher().serverListUpdateTask.reset();
   }

   private DataFetcher.Subscription initDataFetcher(RealmsDataFetcher pDataFetcher) {
      DataFetcher.Subscription datafetcher$subscription = pDataFetcher.dataFetcher.createSubscription();
      datafetcher$subscription.subscribe(pDataFetcher.serverListUpdateTask, (p_296033_) -> {
         this.serverList.updateServersList(p_296033_);
         this.updateLayout(this.serverList.isEmpty() && this.notifications.isEmpty() ? RealmsMainScreen.LayoutState.NO_REALMS : RealmsMainScreen.LayoutState.LIST);
         this.refreshRealmsSelectionList();
         boolean flag = false;

         for(RealmsServer realmsserver : this.serverList) {
            if (this.isSelfOwnedNonExpiredServer(realmsserver)) {
               flag = true;
            }
         }

         if (!regionsPinged && flag) {
            regionsPinged = true;
            this.pingRegions();
         }

      });
      callRealmsClient(RealmsClient::getNotifications, (p_274622_) -> {
         this.notifications.clear();
         this.notifications.addAll(p_274622_);
         if (!this.notifications.isEmpty() && this.activeLayoutState != RealmsMainScreen.LayoutState.LOADING) {
            this.updateLayout(RealmsMainScreen.LayoutState.LIST);
            this.refreshRealmsSelectionList();
         }

      });
      datafetcher$subscription.subscribe(pDataFetcher.pendingInvitesTask, (p_296027_) -> {
         this.pendingInvitesButton.setNotificationCount(p_296027_);
         this.pendingInvitesButton.setTooltip(p_296027_ == 0 ? NO_PENDING_INVITES : PENDING_INVITES);
         if (p_296027_ > 0 && this.inviteNarrationLimiter.tryAcquire(1)) {
            this.minecraft.getNarrator().sayNow(Component.translatable("mco.configure.world.invite.narration", p_296027_));
         }

      });
      datafetcher$subscription.subscribe(pDataFetcher.trialAvailabilityTask, (p_296031_) -> {
         this.trialsAvailable = p_296031_;
      });
      datafetcher$subscription.subscribe(pDataFetcher.newsTask, (p_296037_) -> {
         pDataFetcher.newsManager.updateUnreadNews(p_296037_);
         this.newsLink = pDataFetcher.newsManager.newsLink();
         this.newsButton.setNotificationCount(pDataFetcher.newsManager.hasUnreadNews() ? Integer.MAX_VALUE : 0);
      });
      return datafetcher$subscription;
   }

   private static <T> void callRealmsClient(RealmsMainScreen.RealmsCall<T> pCall, Consumer<T> pOnFinish) {
      Minecraft minecraft = Minecraft.getInstance();
      CompletableFuture.supplyAsync(() -> {
         try {
            return pCall.request(RealmsClient.create(minecraft));
         } catch (RealmsServiceException realmsserviceexception) {
            throw new RuntimeException(realmsserviceexception);
         }
      }).thenAcceptAsync(pOnFinish, minecraft).exceptionally((p_274626_) -> {
         LOGGER.error("Failed to execute call to Realms Service", p_274626_);
         return null;
      });
   }

   private void refreshRealmsSelectionList() {
      RealmsServer realmsserver = this.getSelectedServer();
      this.realmSelectionList.clear();
      List<UUID> list = new ArrayList<>();

      for(RealmsNotification realmsnotification : this.notifications) {
         this.addEntriesForNotification(this.realmSelectionList, realmsnotification);
         if (!realmsnotification.seen() && !this.handledSeenNotifications.contains(realmsnotification.uuid())) {
            list.add(realmsnotification.uuid());
         }
      }

      if (!list.isEmpty()) {
         callRealmsClient((p_274625_) -> {
            p_274625_.notificationsSeen(list);
            return null;
         }, (p_274630_) -> {
            this.handledSeenNotifications.addAll(list);
         });
      }

      for(RealmsServer realmsserver1 : this.serverList) {
         RealmsMainScreen.ServerEntry realmsmainscreen$serverentry = new RealmsMainScreen.ServerEntry(realmsserver1);
         this.realmSelectionList.addEntry(realmsmainscreen$serverentry);
         if (realmsserver != null && realmsserver.id == realmsserver1.id) {
            this.realmSelectionList.setSelected((RealmsMainScreen.Entry)realmsmainscreen$serverentry);
         }
      }

      this.updateButtonStates();
   }

   private void addEntriesForNotification(RealmsMainScreen.RealmSelectionList pSelectionList, RealmsNotification pNotification) {
      if (pNotification instanceof RealmsNotification.VisitUrl realmsnotification$visiturl) {
         Component component = realmsnotification$visiturl.getMessage();
         int i = this.font.wordWrapHeight(component, 216);
         int j = Mth.positiveCeilDiv(i + 7, 36) - 1;
         pSelectionList.addEntry(new RealmsMainScreen.NotificationMessageEntry(component, j + 2, realmsnotification$visiturl));

         for(int k = 0; k < j; ++k) {
            pSelectionList.addEntry(new RealmsMainScreen.EmptyEntry());
         }

         pSelectionList.addEntry(new RealmsMainScreen.ButtonEntry(realmsnotification$visiturl.buildOpenLinkButton(this)));
      }

   }

   private void pingRegions() {
      (new Thread(() -> {
         List<RegionPingResult> list = Ping.pingAllRegions();
         RealmsClient realmsclient = RealmsClient.create();
         PingResult pingresult = new PingResult();
         pingresult.pingResults = list;
         pingresult.worldIds = this.getOwnedNonExpiredWorldIds();

         try {
            realmsclient.sendPingResults(pingresult);
         } catch (Throwable throwable) {
            LOGGER.warn("Could not send ping result to Realms: ", throwable);
         }

      })).start();
   }

   private List<Long> getOwnedNonExpiredWorldIds() {
      List<Long> list = Lists.newArrayList();

      for(RealmsServer realmsserver : this.serverList) {
         if (this.isSelfOwnedNonExpiredServer(realmsserver)) {
            list.add(realmsserver.id);
         }
      }

      return list;
   }

   private void onRenew(@Nullable RealmsServer pRealmsServer) {
      if (pRealmsServer != null) {
         String s = CommonLinks.extendRealms(pRealmsServer.remoteSubscriptionId, this.minecraft.getUser().getProfileId(), pRealmsServer.expiredTrial);
         this.minecraft.keyboardHandler.setClipboard(s);
         Util.getPlatform().openUri(s);
      }

   }

   private void configureClicked(@Nullable RealmsServer pRealmsServer) {
      if (pRealmsServer != null && this.minecraft.isLocalPlayer(pRealmsServer.ownerUUID)) {
         this.minecraft.setScreen(new RealmsConfigureWorldScreen(this, pRealmsServer.id));
      }

   }

   private void leaveClicked(@Nullable RealmsServer pRealmsServer) {
      if (pRealmsServer != null && !this.minecraft.isLocalPlayer(pRealmsServer.ownerUUID)) {
         Component component = Component.translatable("mco.configure.world.leave.question.line1");
         Component component1 = Component.translatable("mco.configure.world.leave.question.line2");
         this.minecraft.setScreen(new RealmsLongConfirmationScreen((p_231253_) -> {
            this.leaveServer(p_231253_, pRealmsServer);
         }, RealmsLongConfirmationScreen.Type.INFO, component, component1, true));
      }

   }

   @Nullable
   private RealmsServer getSelectedServer() {
      RealmsMainScreen.Entry realmsmainscreen$entry = this.realmSelectionList.getSelected();
      return realmsmainscreen$entry != null ? realmsmainscreen$entry.getServer() : null;
   }

   private void leaveServer(boolean pConfirmed, final RealmsServer pServer) {
      if (pConfirmed) {
         (new Thread("Realms-leave-server") {
            public void run() {
               try {
                  RealmsClient realmsclient = RealmsClient.create();
                  realmsclient.uninviteMyselfFrom(pServer.id);
                  RealmsMainScreen.this.minecraft.execute(() -> {
                     RealmsMainScreen.this.removeServer(pServer);
                  });
               } catch (RealmsServiceException realmsserviceexception) {
                  RealmsMainScreen.LOGGER.error("Couldn't configure world", (Throwable)realmsserviceexception);
                  RealmsMainScreen.this.minecraft.execute(() -> {
                     RealmsMainScreen.this.minecraft.setScreen(new RealmsGenericErrorScreen(realmsserviceexception, RealmsMainScreen.this));
                  });
               }

            }
         }).start();
      }

      this.minecraft.setScreen(this);
   }

   void removeServer(RealmsServer pServer) {
      this.serverList.removeItem(pServer);
      this.realmSelectionList.children().removeIf((p_231250_) -> {
         RealmsServer realmsserver = p_231250_.getServer();
         return realmsserver != null && realmsserver.id == pServer.id;
      });
      this.realmSelectionList.setSelected((RealmsMainScreen.Entry)null);
      this.updateButtonStates();
   }

   void dismissNotification(UUID pUuid) {
      callRealmsClient((p_274628_) -> {
         p_274628_.notificationsDismiss(List.of(pUuid));
         return null;
      }, (p_274632_) -> {
         this.notifications.removeIf((p_274621_) -> {
            return p_274621_.dismissable() && pUuid.equals(p_274621_.uuid());
         });
         this.refreshRealmsSelectionList();
      });
   }

   public void resetScreen() {
      this.realmSelectionList.setSelected((RealmsMainScreen.Entry)null);
   }

   public Component getNarrationMessage() {
      Object object;
      switch (this.activeLayoutState) {
         case LOADING:
            object = CommonComponents.joinForNarration(super.getNarrationMessage(), LOADING_TEXT);
            break;
         case NO_REALMS:
            object = CommonComponents.joinForNarration(super.getNarrationMessage(), NO_REALMS_TEXT);
            break;
         case LIST:
            object = super.getNarrationMessage();
            break;
         default:
            throw new IncompatibleClassChangeError();
      }

      return (Component)object;
   }

   /**
    * Renders the graphical user interface (GUI) element.
    * @param pGuiGraphics the GuiGraphics object used for rendering.
    * @param pMouseX the x-coordinate of the mouse cursor.
    * @param pMouseY the y-coordinate of the mouse cursor.
    * @param pPartialTick the partial tick time.
    */
   public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
      super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
      if (this.trialsAvailable && this.addRealmButton.active) {
         RealmsPopupScreen.renderDiamond(pGuiGraphics, this.addRealmButton);
      }

      switch (RealmsClient.ENVIRONMENT) {
         case STAGE:
            this.renderEnvironment(pGuiGraphics, "STAGE!", -256);
            break;
         case LOCAL:
            this.renderEnvironment(pGuiGraphics, "LOCAL!", 8388479);
      }

   }

   private void openTrialAvailablePopup() {
      this.minecraft.setScreen(new RealmsPopupScreen(this, this.trialsAvailable));
   }

   public static void play(@Nullable RealmsServer pRealmsServer, Screen pLastScreen) {
      if (pRealmsServer != null) {
         Minecraft.getInstance().setScreen(new RealmsLongRunningMcoTaskScreen(pLastScreen, new GetServerDetailsTask(pLastScreen, pRealmsServer)));
      }

   }

   boolean isSelfOwnedServer(RealmsServer pServer) {
      return this.minecraft.isLocalPlayer(pServer.ownerUUID);
   }

   private boolean isSelfOwnedNonExpiredServer(RealmsServer pServer) {
      return this.isSelfOwnedServer(pServer) && !pServer.expired;
   }

   private void renderEnvironment(GuiGraphics pGuiGraphics, String pText, int pColor) {
      pGuiGraphics.pose().pushPose();
      pGuiGraphics.pose().translate((float)(this.width / 2 - 25), 20.0F, 0.0F);
      pGuiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(-20.0F));
      pGuiGraphics.pose().scale(1.5F, 1.5F, 1.5F);
      pGuiGraphics.drawString(this.font, pText, 0, 0, pColor, false);
      pGuiGraphics.pose().popPose();
   }

   @OnlyIn(Dist.CLIENT)
   class ButtonEntry extends RealmsMainScreen.Entry {
      private final Button button;

      public ButtonEntry(Button pButton) {
         this.button = pButton;
      }

      /**
       * Called when a mouse button is clicked within the GUI element.
       * <p>
       * @return {@code true} if the event is consumed, {@code false} otherwise.
       * @param pMouseX the X coordinate of the mouse.
       * @param pMouseY the Y coordinate of the mouse.
       * @param pButton the button that was clicked.
       */
      public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
         this.button.mouseClicked(pMouseX, pMouseY, pButton);
         return true;
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
         return this.button.keyPressed(pKeyCode, pScanCode, pModifiers) ? true : super.keyPressed(pKeyCode, pScanCode, pModifiers);
      }

      public void render(GuiGraphics pGuiGraphics, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pHovering, float pPartialTick) {
         this.button.setPosition(RealmsMainScreen.this.width / 2 - 75, pTop + 4);
         this.button.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
      }

      public Component getNarration() {
         return this.button.getMessage();
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class CrossButton extends ImageButton {
      private static final WidgetSprites SPRITES = new WidgetSprites(new ResourceLocation("widget/cross_button"), new ResourceLocation("widget/cross_button_highlighted"));

      protected CrossButton(Button.OnPress pOnPress, Component pMessage) {
         super(0, 0, 14, 14, SPRITES, pOnPress);
         this.setTooltip(Tooltip.create(pMessage));
      }
   }

   @OnlyIn(Dist.CLIENT)
   class EmptyEntry extends RealmsMainScreen.Entry {
      public void render(GuiGraphics p_301870_, int p_301858_, int p_301868_, int p_301866_, int p_301860_, int p_301859_, int p_301864_, int p_301865_, boolean p_301869_, float p_301861_) {
      }

      public Component getNarration() {
         return Component.empty();
      }
   }

   @OnlyIn(Dist.CLIENT)
   abstract class Entry extends ObjectSelectionList.Entry<RealmsMainScreen.Entry> {
      @Nullable
      public RealmsServer getServer() {
         return null;
      }
   }

   @OnlyIn(Dist.CLIENT)
   static enum LayoutState {
      LOADING,
      NO_REALMS,
      LIST;
   }

   @OnlyIn(Dist.CLIENT)
   static class NotificationButton extends SpriteIconButton.CenteredIcon {
      private static final ResourceLocation[] NOTIFICATION_ICONS = new ResourceLocation[]{new ResourceLocation("notification/1"), new ResourceLocation("notification/2"), new ResourceLocation("notification/3"), new ResourceLocation("notification/4"), new ResourceLocation("notification/5"), new ResourceLocation("notification/more")};
      private static final int UNKNOWN_COUNT = Integer.MAX_VALUE;
      private static final int SIZE = 20;
      private static final int SPRITE_SIZE = 14;
      private int notificationCount;

      public NotificationButton(Component pMessage, ResourceLocation pSprite, Button.OnPress pOnPress) {
         super(20, 20, pMessage, 14, 14, pSprite, pOnPress);
      }

      int notificationCount() {
         return this.notificationCount;
      }

      public void setNotificationCount(int pNotificationCount) {
         this.notificationCount = pNotificationCount;
      }

      public void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
         super.renderWidget(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
         if (this.active && this.notificationCount != 0) {
            this.drawNotificationCounter(pGuiGraphics);
         }

      }

      private void drawNotificationCounter(GuiGraphics pGuiGraphics) {
         pGuiGraphics.blitSprite(NOTIFICATION_ICONS[Math.min(this.notificationCount, 6) - 1], this.getX() + this.getWidth() - 5, this.getY() - 3, 8, 8);
      }
   }

   @OnlyIn(Dist.CLIENT)
   class NotificationMessageEntry extends RealmsMainScreen.Entry {
      private static final int SIDE_MARGINS = 40;
      private static final int OUTLINE_COLOR = -12303292;
      private final Component text;
      private final int frameItemHeight;
      private final List<AbstractWidget> children = new ArrayList<>();
      @Nullable
      private final RealmsMainScreen.CrossButton dismissButton;
      private final MultiLineTextWidget textWidget;
      private final GridLayout gridLayout;
      private final FrameLayout textFrame;
      private int lastEntryWidth = -1;

      public NotificationMessageEntry(Component pText, int pFrameItemHeight, RealmsNotification pNotification) {
         this.text = pText;
         this.frameItemHeight = pFrameItemHeight;
         this.gridLayout = new GridLayout();
         int i = 7;
         this.gridLayout.addChild(ImageWidget.sprite(20, 20, RealmsMainScreen.INFO_SPRITE), 0, 0, this.gridLayout.newCellSettings().padding(7, 7, 0, 0));
         this.gridLayout.addChild(SpacerElement.width(40), 0, 0);
         this.textFrame = this.gridLayout.addChild(new FrameLayout(0, 9 * 3 * (pFrameItemHeight - 1)), 0, 1, this.gridLayout.newCellSettings().paddingTop(7));
         this.textWidget = this.textFrame.addChild((new MultiLineTextWidget(pText, RealmsMainScreen.this.font)).setCentered(true), this.textFrame.newChildLayoutSettings().alignHorizontallyCenter().alignVerticallyTop());
         this.gridLayout.addChild(SpacerElement.width(40), 0, 2);
         if (pNotification.dismissable()) {
            this.dismissButton = this.gridLayout.addChild(new RealmsMainScreen.CrossButton((p_275478_) -> {
               RealmsMainScreen.this.dismissNotification(pNotification.uuid());
            }, Component.translatable("mco.notification.dismiss")), 0, 2, this.gridLayout.newCellSettings().alignHorizontallyRight().padding(0, 7, 7, 0));
         } else {
            this.dismissButton = null;
         }

         this.gridLayout.visitWidgets(this.children::add);
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
         return this.dismissButton != null && this.dismissButton.keyPressed(pKeyCode, pScanCode, pModifiers) ? true : super.keyPressed(pKeyCode, pScanCode, pModifiers);
      }

      private void updateEntryWidth(int pEntryWidth) {
         if (this.lastEntryWidth != pEntryWidth) {
            this.refreshLayout(pEntryWidth);
            this.lastEntryWidth = pEntryWidth;
         }

      }

      private void refreshLayout(int pWidth) {
         int i = pWidth - 80;
         this.textFrame.setMinWidth(i);
         this.textWidget.setMaxWidth(i);
         this.gridLayout.arrangeElements();
      }

      public void renderBack(GuiGraphics pGuiGraphics, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pIsMouseOver, float pPartialTick) {
         super.renderBack(pGuiGraphics, pIndex, pTop, pLeft, pWidth, pHeight, pMouseX, pMouseY, pIsMouseOver, pPartialTick);
         pGuiGraphics.renderOutline(pLeft - 2, pTop - 2, pWidth, 36 * this.frameItemHeight - 2, -12303292);
      }

      public void render(GuiGraphics pGuiGraphics, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pHovering, float pPartialTick) {
         this.gridLayout.setPosition(pLeft, pTop);
         this.updateEntryWidth(pWidth - 4);
         this.children.forEach((p_280688_) -> {
            p_280688_.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
         });
      }

      /**
       * Called when a mouse button is clicked within the GUI element.
       * <p>
       * @return {@code true} if the event is consumed, {@code false} otherwise.
       * @param pMouseX the X coordinate of the mouse.
       * @param pMouseY the Y coordinate of the mouse.
       * @param pButton the button that was clicked.
       */
      public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
         if (this.dismissButton != null) {
            this.dismissButton.mouseClicked(pMouseX, pMouseY, pButton);
         }

         return true;
      }

      public Component getNarration() {
         return this.text;
      }
   }

   @OnlyIn(Dist.CLIENT)
   class RealmSelectionList extends RealmsObjectSelectionList<RealmsMainScreen.Entry> {
      public RealmSelectionList() {
         super(RealmsMainScreen.this.width, RealmsMainScreen.this.height, 0, RealmsMainScreen.this.height, 36);
      }

      public void setSelected(@Nullable RealmsMainScreen.Entry p_86849_) {
         super.setSelected(p_86849_);
         RealmsMainScreen.this.updateButtonStates();
      }

      public int getMaxPosition() {
         return this.getItemCount() * 36;
      }

      public int getRowWidth() {
         return 300;
      }
   }

   @OnlyIn(Dist.CLIENT)
   interface RealmsCall<T> {
      T request(RealmsClient pRealmsClient) throws RealmsServiceException;
   }

   @OnlyIn(Dist.CLIENT)
   class ServerEntry extends RealmsMainScreen.Entry {
      private static final int SKIN_HEAD_LARGE_WIDTH = 36;
      private final RealmsServer serverData;

      public ServerEntry(RealmsServer pServerData) {
         this.serverData = pServerData;
      }

      public void render(GuiGraphics pGuiGraphics, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pHovering, float pPartialTick) {
         if (this.serverData.state == RealmsServer.State.UNINITIALIZED) {
            pGuiGraphics.blitSprite(RealmsMainScreen.NEW_REALM_SPRITE, pLeft + 36 + 10, pTop + 6, 40, 20);
            int i1 = pLeft + 36 + 10 + 40 + 10;
            pGuiGraphics.drawString(RealmsMainScreen.this.font, RealmsMainScreen.SERVER_UNITIALIZED_TEXT, i1, pTop + 12, -1);
         } else {
            int i = 225;
            int j = 2;
            this.renderStatusLights(this.serverData, pGuiGraphics, pLeft + 36, pTop, pMouseX, pMouseY, 225, 2);
            if (RealmsMainScreen.this.isSelfOwnedServer(this.serverData) && this.serverData.expired) {
               Component component = this.serverData.expiredTrial ? RealmsMainScreen.TRIAL_EXPIRED_TEXT : RealmsMainScreen.SUBSCRIPTION_EXPIRED_TEXT;
               int j1 = pTop + 11 + 5;
               pGuiGraphics.drawString(RealmsMainScreen.this.font, component, pLeft + 36 + 2, j1 + 1, 15553363, false);
            } else {
               if (this.serverData.worldType == RealmsServer.WorldType.MINIGAME) {
                  int k = 13413468;
                  int l = RealmsMainScreen.this.font.width(RealmsMainScreen.SELECT_MINIGAME_PREFIX);
                  pGuiGraphics.drawString(RealmsMainScreen.this.font, RealmsMainScreen.SELECT_MINIGAME_PREFIX, pLeft + 36 + 2, pTop + 12, 13413468, false);
                  pGuiGraphics.drawString(RealmsMainScreen.this.font, this.serverData.getMinigameName(), pLeft + 36 + 2 + l, pTop + 12, 7105644, false);
               } else {
                  pGuiGraphics.drawString(RealmsMainScreen.this.font, this.serverData.getDescription(), pLeft + 36 + 2, pTop + 12, 7105644, false);
               }

               if (!RealmsMainScreen.this.isSelfOwnedServer(this.serverData)) {
                  pGuiGraphics.drawString(RealmsMainScreen.this.font, this.serverData.owner, pLeft + 36 + 2, pTop + 12 + 11, 5000268, false);
               }
            }

            pGuiGraphics.drawString(RealmsMainScreen.this.font, this.serverData.getName(), pLeft + 36 + 2, pTop + 1, -1, false);
            RealmsUtil.renderPlayerFace(pGuiGraphics, pLeft + 36 - 36, pTop, 32, this.serverData.ownerUUID);
         }
      }

      private void playRealm() {
         RealmsMainScreen.this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
         RealmsMainScreen.play(this.serverData, RealmsMainScreen.this);
      }

      private void createUnitializedRealm() {
         RealmsMainScreen.this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
         RealmsCreateRealmScreen realmscreaterealmscreen = new RealmsCreateRealmScreen(this.serverData, RealmsMainScreen.this);
         RealmsMainScreen.this.minecraft.setScreen(realmscreaterealmscreen);
      }

      /**
       * Called when a mouse button is clicked within the GUI element.
       * <p>
       * @return {@code true} if the event is consumed, {@code false} otherwise.
       * @param pMouseX the X coordinate of the mouse.
       * @param pMouseY the Y coordinate of the mouse.
       * @param pButton the button that was clicked.
       */
      public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
         if (this.serverData.state == RealmsServer.State.UNINITIALIZED) {
            this.createUnitializedRealm();
         } else if (RealmsMainScreen.this.shouldPlayButtonBeActive(this.serverData)) {
            if (Util.getMillis() - RealmsMainScreen.this.lastClickTime < 250L && this.isFocused()) {
               this.playRealm();
            }

            RealmsMainScreen.this.lastClickTime = Util.getMillis();
         }

         return true;
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
         if (CommonInputs.selected(pKeyCode)) {
            if (this.serverData.state == RealmsServer.State.UNINITIALIZED) {
               this.createUnitializedRealm();
               return true;
            }

            if (RealmsMainScreen.this.shouldPlayButtonBeActive(this.serverData)) {
               this.playRealm();
               return true;
            }
         }

         return super.keyPressed(pKeyCode, pScanCode, pModifiers);
      }

      private void renderStatusLights(RealmsServer pRealmsServer, GuiGraphics pGuiGraphics, int pX, int pY, int pMouseX, int pMouseY, int pWidth, int pHeight) {
         int i = pX + pWidth + 22;
         if (pRealmsServer.expired) {
            this.drawRealmStatus(pGuiGraphics, i, pY + pHeight, pMouseX, pMouseY, RealmsMainScreen.EXPIRED_SPRITE, () -> {
               return RealmsMainScreen.SERVER_EXPIRED_TOOLTIP;
            });
         } else if (pRealmsServer.state == RealmsServer.State.CLOSED) {
            this.drawRealmStatus(pGuiGraphics, i, pY + pHeight, pMouseX, pMouseY, RealmsMainScreen.CLOSED_SPRITE, () -> {
               return RealmsMainScreen.SERVER_CLOSED_TOOLTIP;
            });
         } else if (RealmsMainScreen.this.isSelfOwnedServer(pRealmsServer) && pRealmsServer.daysLeft < 7) {
            this.drawRealmStatus(pGuiGraphics, i, pY + pHeight, pMouseX, pMouseY, RealmsMainScreen.EXPIRES_SOON_SPRITE, () -> {
               if (pRealmsServer.daysLeft <= 0) {
                  return RealmsMainScreen.SERVER_EXPIRES_SOON_TOOLTIP;
               } else {
                  return (Component)(pRealmsServer.daysLeft == 1 ? RealmsMainScreen.SERVER_EXPIRES_IN_DAY_TOOLTIP : Component.translatable("mco.selectServer.expires.days", pRealmsServer.daysLeft));
               }
            });
         } else if (pRealmsServer.state == RealmsServer.State.OPEN) {
            this.drawRealmStatus(pGuiGraphics, i, pY + pHeight, pMouseX, pMouseY, RealmsMainScreen.OPEN_SPRITE, () -> {
               return RealmsMainScreen.SERVER_OPEN_TOOLTIP;
            });
         }

      }

      private void drawRealmStatus(GuiGraphics pGuiGraphics, int pX, int pY, int pMouseX, int pMouseY, ResourceLocation pSprite, Supplier<Component> pTooltipSupplier) {
         pGuiGraphics.blitSprite(pSprite, pX, pY, 10, 28);
         if (pMouseX >= pX && pMouseX <= pX + 9 && pMouseY >= pY && pMouseY <= pY + 27 && pMouseY < RealmsMainScreen.this.height - 40 && pMouseY > 32) {
            RealmsMainScreen.this.setTooltipForNextRenderPass(pTooltipSupplier.get());
         }

      }

      public Component getNarration() {
         return (Component)(this.serverData.state == RealmsServer.State.UNINITIALIZED ? RealmsMainScreen.UNITIALIZED_WORLD_NARRATION : Component.translatable("narrator.select", this.serverData.name));
      }

      @Nullable
      public RealmsServer getServer() {
         return this.serverData;
      }
   }
}