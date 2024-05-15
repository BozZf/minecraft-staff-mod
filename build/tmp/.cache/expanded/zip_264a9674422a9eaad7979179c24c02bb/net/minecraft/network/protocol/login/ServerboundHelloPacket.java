package net.minecraft.network.protocol.login;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public record ServerboundHelloPacket(String name, UUID profileId) implements Packet<ServerLoginPacketListener> {
   public ServerboundHelloPacket(FriendlyByteBuf pBuffer) {
      this(pBuffer.readUtf(16), pBuffer.readUUID());
   }

   /**
    * Writes the raw packet data to the data stream.
    */
   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeUtf(this.name, 16);
      pBuffer.writeUUID(this.profileId);
   }

   /**
    * Passes this Packet on to the NetHandler for processing.
    */
   public void handle(ServerLoginPacketListener pHandler) {
      pHandler.handleHello(this);
   }
}