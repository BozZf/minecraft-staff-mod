package net.minecraft.network.protocol.login;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public record ServerboundLoginAcknowledgedPacket() implements Packet<ServerLoginPacketListener> {
   public ServerboundLoginAcknowledgedPacket(FriendlyByteBuf pBuffer) {
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
   public void handle(ServerLoginPacketListener pHandler) {
      pHandler.handleLoginAcknowledgement(this);
   }

   public ConnectionProtocol nextProtocol() {
      return ConnectionProtocol.CONFIGURATION;
   }
}