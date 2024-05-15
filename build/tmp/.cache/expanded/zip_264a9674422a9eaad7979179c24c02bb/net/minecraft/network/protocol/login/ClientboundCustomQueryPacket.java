package net.minecraft.network.protocol.login;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.login.custom.CustomQueryPayload;
import net.minecraft.network.protocol.login.custom.DiscardedQueryPayload;
import net.minecraft.resources.ResourceLocation;

public record ClientboundCustomQueryPacket(int transactionId, CustomQueryPayload payload) implements Packet<ClientLoginPacketListener>, net.minecraftforge.network.ICustomPacket<ClientboundCustomQueryPacket> {
   private static final int MAX_PAYLOAD_SIZE = 1048576;

   public ClientboundCustomQueryPacket(FriendlyByteBuf pBuffer) {
      this(pBuffer.readVarInt(), readPayload(pBuffer.readResourceLocation(), pBuffer));
   }

   private static CustomQueryPayload readPayload(ResourceLocation pId, FriendlyByteBuf pBuffer) {
      return readUnknownPayload(pId, pBuffer);
   }

   private static DiscardedQueryPayload readUnknownPayload(ResourceLocation pId, FriendlyByteBuf pBuffer) {
      int i = pBuffer.readableBytes();
      if (i >= 0 && i <= 1048576) {
         return new DiscardedQueryPayload(pId, new FriendlyByteBuf(pBuffer.readBytes(i)));
      } else {
         throw new IllegalArgumentException("Payload may not be larger than 1048576 bytes");
      }
   }

   /**
    * Writes the raw packet data to the data stream.
    */
   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeVarInt(this.transactionId);
      pBuffer.writeResourceLocation(this.payload.id());
      this.payload.write(pBuffer);
   }

   /**
    * Passes this Packet on to the NetHandler for processing.
    */
   public void handle(ClientLoginPacketListener pHandler) {
      pHandler.handleCustomQuery(this);
   }

   @Override public int getIndex() { return transactionId(); }
   @Override public ResourceLocation getName() { return this.payload.id(); }
   @org.jetbrains.annotations.Nullable @Override public FriendlyByteBuf getInternalData() { return ((DiscardedQueryPayload)this.payload).data(); }
}
