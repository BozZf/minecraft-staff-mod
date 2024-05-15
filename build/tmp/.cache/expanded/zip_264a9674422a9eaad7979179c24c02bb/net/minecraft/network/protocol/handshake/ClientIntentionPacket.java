package net.minecraft.network.protocol.handshake;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public record ClientIntentionPacket(int protocolVersion, String hostName, int port, ClientIntent intention) implements Packet<ServerHandshakePacketListener> {
   private static final int MAX_HOST_LENGTH = 255;

   public ClientIntentionPacket(FriendlyByteBuf pBuffer) {
      this(pBuffer.readVarInt(), pBuffer.readUtf(255), pBuffer.readUnsignedShort(), ClientIntent.byId(pBuffer.readVarInt()));
   }

   /**
    * Writes the raw packet data to the data stream.
    */
   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeVarInt(this.protocolVersion);
      pBuffer.writeUtf(this.hostName);
      pBuffer.writeShort(this.port);
      pBuffer.writeVarInt(this.intention.id());
   }

   /**
    * Passes this Packet on to the NetHandler for processing.
    */
   public void handle(ServerHandshakePacketListener pHandler) {
      pHandler.handleIntention(this);
   }

   public ConnectionProtocol nextProtocol() {
      return this.intention.protocol();
   }
}