package net.minecraft.network.protocol.configuration;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public record ClientboundFinishConfigurationPacket() implements Packet<ClientConfigurationPacketListener> {
   public ClientboundFinishConfigurationPacket(FriendlyByteBuf pBuffer) {
      this();
   }

   /**
    * Writes the raw packet data to the data stream.
    */
   public void write(FriendlyByteBuf pBuffer) {
   }

   /**
    * Passes this Packet on to the NetHandler for processing.
    */
   public void handle(ClientConfigurationPacketListener pHandler) {
      pHandler.handleConfigurationFinished(this);
   }

   public ConnectionProtocol nextProtocol() {
      return ConnectionProtocol.PLAY;
   }
}