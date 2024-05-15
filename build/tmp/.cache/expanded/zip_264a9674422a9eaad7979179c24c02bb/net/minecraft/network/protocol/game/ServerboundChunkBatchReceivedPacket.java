package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public record ServerboundChunkBatchReceivedPacket(float desiredChunksPerTick) implements Packet<ServerGamePacketListener> {
   public ServerboundChunkBatchReceivedPacket(FriendlyByteBuf pBuffer) {
      this(pBuffer.readFloat());
   }

   /**
    * Writes the raw packet data to the data stream.
    */
   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeFloat(this.desiredChunksPerTick);
   }

   /**
    * Passes this Packet on to the NetHandler for processing.
    */
   public void handle(ServerGamePacketListener pHandler) {
      pHandler.handleChunkBatchReceived(this);
   }
}