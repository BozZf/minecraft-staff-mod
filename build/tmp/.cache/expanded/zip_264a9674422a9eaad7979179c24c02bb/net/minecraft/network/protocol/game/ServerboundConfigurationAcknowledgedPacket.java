package net.minecraft.network.protocol.game;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public record ServerboundConfigurationAcknowledgedPacket() implements Packet<ServerGamePacketListener> {
   public ServerboundConfigurationAcknowledgedPacket(FriendlyByteBuf pBuffer) {
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
   public void handle(ServerGamePacketListener pHandler) {
      pHandler.handleConfigurationAcknowledged(this);
   }

   public ConnectionProtocol nextProtocol() {
      return ConnectionProtocol.CONFIGURATION;
   }
}