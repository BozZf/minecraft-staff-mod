package net.minecraft.client.multiplayer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.telemetry.WorldSessionTelemetryManager;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.Connection;
import net.minecraft.network.ServerboundPacketListener;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.common.ClientCommonPacketListener;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.common.ClientboundResourcePackPacket;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.realms.DisconnectedRealmsScreen;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagNetworkSerialization;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public abstract class ClientCommonPacketListenerImpl implements ClientCommonPacketListener {
   private static final Component GENERIC_DISCONNECT_MESSAGE = Component.translatable("disconnect.lost");
   private static final Logger LOGGER = LogUtils.getLogger();
   protected final Minecraft minecraft;
   protected final Connection connection;
   @Nullable
   protected final ServerData serverData;
   @Nullable
   protected String serverBrand;
   protected final WorldSessionTelemetryManager telemetryManager;
   @Nullable
   protected final Screen postDisconnectScreen;
   private final List<ClientCommonPacketListenerImpl.DeferredPacket> deferredPackets = new ArrayList<>();

   protected ClientCommonPacketListenerImpl(Minecraft pMinecraft, Connection pConnection, CommonListenerCookie pCommonListenerCookie) {
      this.minecraft = pMinecraft;
      this.connection = pConnection;
      this.serverData = pCommonListenerCookie.serverData();
      this.serverBrand = pCommonListenerCookie.serverBrand();
      this.telemetryManager = pCommonListenerCookie.telemetryManager();
      this.postDisconnectScreen = pCommonListenerCookie.postDisconnectScreen();
   }

   public void handleKeepAlive(ClientboundKeepAlivePacket pPacket) {
      this.sendWhen(new ServerboundKeepAlivePacket(pPacket.getId()), () -> {
         return !RenderSystem.isFrozenAtPollEvents();
      }, Duration.ofMinutes(1L));
   }

   public void handlePing(ClientboundPingPacket pPacket) {
      PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
      this.send(new ServerboundPongPacket(pPacket.getId()));
   }

   public void handleCustomPayload(ClientboundCustomPayloadPacket pPacket) {
      if (net.minecraftforge.common.ForgeHooks.onCustomPayload(pPacket, this.connection)) return;
      CustomPacketPayload custompacketpayload = pPacket.payload();
      if (!(custompacketpayload instanceof DiscardedPayload)) {
         PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
         if (custompacketpayload instanceof BrandPayload) {
            BrandPayload brandpayload = (BrandPayload)custompacketpayload;
            this.serverBrand = brandpayload.brand();
            this.telemetryManager.onServerBrandReceived(brandpayload.brand());
         } else {
            this.handleCustomPayload(custompacketpayload);
         }

      }
   }

   protected abstract void handleCustomPayload(CustomPacketPayload pPayload);

   protected abstract RegistryAccess.Frozen registryAccess();

   public void handleResourcePack(ClientboundResourcePackPacket pPacket) {
      URL url = parseResourcePackUrl(pPacket.getUrl());
      if (url == null) {
         this.send(ServerboundResourcePackPacket.Action.FAILED_DOWNLOAD);
      } else {
         String s = pPacket.getHash();
         boolean flag = pPacket.isRequired();
         if (this.serverData != null && this.serverData.getResourcePackStatus() == ServerData.ServerPackStatus.ENABLED) {
            this.send(ServerboundResourcePackPacket.Action.ACCEPTED);
            this.packApplicationCallback(this.minecraft.getDownloadedPackSource().downloadAndSelectResourcePack(url, s, true));
         } else if (this.serverData != null && this.serverData.getResourcePackStatus() != ServerData.ServerPackStatus.PROMPT && (!flag || this.serverData.getResourcePackStatus() != ServerData.ServerPackStatus.DISABLED)) {
            this.send(ServerboundResourcePackPacket.Action.DECLINED);
            if (flag) {
               this.connection.disconnect(Component.translatable("multiplayer.requiredTexturePrompt.disconnect"));
            }
         } else {
            this.minecraft.execute(() -> {
               this.showServerPackPrompt(url, s, flag, pPacket.getPrompt());
            });
         }

      }
   }

   private void showServerPackPrompt(URL pResourcePackUrl, String pHash, boolean pRequired, @Nullable Component pPrompt) {
      Screen screen = this.minecraft.screen;
      this.minecraft.setScreen(new ConfirmScreen((p_298595_) -> {
         this.minecraft.setScreen(screen);
         if (p_298595_) {
            if (this.serverData != null) {
               this.serverData.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);
            }

            this.send(ServerboundResourcePackPacket.Action.ACCEPTED);
            this.packApplicationCallback(this.minecraft.getDownloadedPackSource().downloadAndSelectResourcePack(pResourcePackUrl, pHash, true));
         } else {
            this.send(ServerboundResourcePackPacket.Action.DECLINED);
            if (pRequired) {
               this.connection.disconnect(Component.translatable("multiplayer.requiredTexturePrompt.disconnect"));
            } else if (this.serverData != null) {
               this.serverData.setResourcePackStatus(ServerData.ServerPackStatus.DISABLED);
            }
         }

         if (this.serverData != null) {
            ServerList.saveSingleServer(this.serverData);
         }

      }, pRequired ? Component.translatable("multiplayer.requiredTexturePrompt.line1") : Component.translatable("multiplayer.texturePrompt.line1"), preparePackPrompt(pRequired ? Component.translatable("multiplayer.requiredTexturePrompt.line2").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD) : Component.translatable("multiplayer.texturePrompt.line2"), pPrompt), pRequired ? CommonComponents.GUI_PROCEED : CommonComponents.GUI_YES, (Component)(pRequired ? Component.translatable("menu.disconnect") : CommonComponents.GUI_NO)));
   }

   private static Component preparePackPrompt(Component pLine1, @Nullable Component pLine2) {
      return (Component)(pLine2 == null ? pLine1 : Component.translatable("multiplayer.texturePrompt.serverPrompt", pLine1, pLine2));
   }

   @Nullable
   private static URL parseResourcePackUrl(String pUrl) {
      try {
         URL url = new URL(pUrl);
         String s = url.getProtocol();
         return !"http".equals(s) && !"https".equals(s) ? null : url;
      } catch (MalformedURLException malformedurlexception) {
         return null;
      }
   }

   private void packApplicationCallback(CompletableFuture<?> pFuture) {
      pFuture.thenRun(() -> {
         this.send(ServerboundResourcePackPacket.Action.SUCCESSFULLY_LOADED);
      }).exceptionally((p_299077_) -> {
         this.send(ServerboundResourcePackPacket.Action.FAILED_DOWNLOAD);
         return null;
      });
   }

   public void handleUpdateTags(ClientboundUpdateTagsPacket pPacket) {
      PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
      pPacket.getTags().forEach(this::updateTagsForRegistry);
   }

   private <T> void updateTagsForRegistry(ResourceKey<? extends Registry<? extends T>> p_301094_, TagNetworkSerialization.NetworkPayload p_297701_) {
      if (!p_297701_.isEmpty()) {
         Registry<T> registry = this.registryAccess().registry(p_301094_).orElseThrow(() -> {
            return new IllegalStateException("Unknown registry " + p_301094_);
         });
         Map<TagKey<T>, List<Holder<T>>> map = new HashMap<>();
         TagNetworkSerialization.deserializeTagsFromNetwork((ResourceKey<? extends Registry<T>>)p_301094_, registry, p_297701_, map::put);
         registry.bindTags(map);
      }
   }

   private void send(ServerboundResourcePackPacket.Action pAction) {
      this.connection.send(new ServerboundResourcePackPacket(pAction));
   }

   public void handleDisconnect(ClientboundDisconnectPacket pPacket) {
      this.connection.disconnect(pPacket.getReason());
   }

   protected void sendDeferredPackets() {
      Iterator<ClientCommonPacketListenerImpl.DeferredPacket> iterator = this.deferredPackets.iterator();

      while(iterator.hasNext()) {
         ClientCommonPacketListenerImpl.DeferredPacket clientcommonpacketlistenerimpl$deferredpacket = iterator.next();
         if (clientcommonpacketlistenerimpl$deferredpacket.sendCondition().getAsBoolean()) {
            this.send(clientcommonpacketlistenerimpl$deferredpacket.packet);
            iterator.remove();
         } else if (clientcommonpacketlistenerimpl$deferredpacket.expirationTime() <= Util.getMillis()) {
            iterator.remove();
         }
      }

   }

   public void send(Packet<?> pPacket) {
      this.connection.send(pPacket);
   }

   /**
    * Invoked when disconnecting, the parameter is a ChatComponent describing the reason for termination
    */
   public void onDisconnect(Component pReason) {
      this.telemetryManager.onDisconnect();
      this.minecraft.disconnect(this.createDisconnectScreen(pReason));
      LOGGER.warn("Client disconnected with reason: {}", (Object)pReason.getString());
   }

   protected Screen createDisconnectScreen(Component pReason) {
      Screen screen = Objects.requireNonNullElseGet(this.postDisconnectScreen, () -> {
         return new JoinMultiplayerScreen(new TitleScreen());
      });
      return (Screen)(this.serverData != null && this.serverData.isRealm() ? new DisconnectedRealmsScreen(screen, GENERIC_DISCONNECT_MESSAGE, pReason) : new DisconnectedScreen(screen, GENERIC_DISCONNECT_MESSAGE, pReason));
   }

   @Nullable
   public String serverBrand() {
      return this.serverBrand;
   }

   private void sendWhen(Packet<? extends ServerboundPacketListener> pPacket, BooleanSupplier pSendCondition, Duration pExpirationTime) {
      if (pSendCondition.getAsBoolean()) {
         this.send(pPacket);
      } else {
         this.deferredPackets.add(new ClientCommonPacketListenerImpl.DeferredPacket(pPacket, pSendCondition, Util.getMillis() + pExpirationTime.toMillis()));
      }

   }

   @OnlyIn(Dist.CLIENT)
   static record DeferredPacket(Packet<? extends ServerboundPacketListener> packet, BooleanSupplier sendCondition, long expirationTime) {
   }
}
