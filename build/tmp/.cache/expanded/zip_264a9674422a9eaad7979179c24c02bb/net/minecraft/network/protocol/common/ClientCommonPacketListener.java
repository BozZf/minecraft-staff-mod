package net.minecraft.network.protocol.common;

import net.minecraft.network.ClientboundPacketListener;

public interface ClientCommonPacketListener extends ClientboundPacketListener {
   void handleKeepAlive(ClientboundKeepAlivePacket pPacket);

   void handlePing(ClientboundPingPacket pPacket);

   void handleCustomPayload(ClientboundCustomPayloadPacket pPacket);

   void handleDisconnect(ClientboundDisconnectPacket pPacket);

   void handleResourcePack(ClientboundResourcePackPacket pPacket);

   void handleUpdateTags(ClientboundUpdateTagsPacket pPacket);
}