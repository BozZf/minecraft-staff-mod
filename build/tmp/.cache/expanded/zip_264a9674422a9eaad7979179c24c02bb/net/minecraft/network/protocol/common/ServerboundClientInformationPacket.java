package net.minecraft.network.protocol.common;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ClientInformation;

public record ServerboundClientInformationPacket(ClientInformation information) implements Packet<ServerCommonPacketListener> {
   public ServerboundClientInformationPacket(FriendlyByteBuf pBuffer) {
      this(new ClientInformation(pBuffer));
   }

   /**
    * Writes the raw packet data to the data stream.
    */
   public void write(FriendlyByteBuf pBuffer) {
      this.information.write(pBuffer);
   }

   /**
    * Passes this Packet on to the NetHandler for processing.
    */
   public void handle(ServerCommonPacketListener pHandler) {
      pHandler.handleClientInformation(this);
   }
}