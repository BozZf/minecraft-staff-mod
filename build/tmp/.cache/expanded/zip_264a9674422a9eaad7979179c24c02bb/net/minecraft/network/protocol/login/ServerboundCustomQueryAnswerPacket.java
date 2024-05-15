package net.minecraft.network.protocol.login;

import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.login.custom.CustomQueryAnswerPayload;
import net.minecraft.network.protocol.login.custom.DiscardedQueryAnswerPayload;

public record ServerboundCustomQueryAnswerPacket(int transactionId, @Nullable CustomQueryAnswerPayload payload) implements Packet<ServerLoginPacketListener>, net.minecraftforge.network.ICustomPacket<ServerboundCustomQueryAnswerPacket> {
   private static final int MAX_PAYLOAD_SIZE = 1048576;

   public static ServerboundCustomQueryAnswerPacket read(FriendlyByteBuf pBuffer) {
      int i = pBuffer.readVarInt();
      return new ServerboundCustomQueryAnswerPacket(i, readPayload(i, pBuffer));
   }

   private static CustomQueryAnswerPayload readPayload(int pTransactionId, FriendlyByteBuf pBuffer) {
      return readUnknownPayload(pBuffer);
   }

   private static CustomQueryAnswerPayload readUnknownPayload(FriendlyByteBuf pBuffer) {
      int i = pBuffer.readableBytes();
      if (i >= 0 && i <= 1048576) {
         return new DiscardedQueryAnswerPayload(new FriendlyByteBuf(pBuffer.readBytes(i)));
      } else {
         throw new IllegalArgumentException("Payload may not be larger than 1048576 bytes");
      }
   }

   /**
    * Writes the raw packet data to the data stream.
    */
   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeVarInt(this.transactionId);
      pBuffer.writeNullable(this.payload, (p_300758_, p_298999_) -> {
         p_298999_.write(p_300758_);
      });
   }

   /**
    * Passes this Packet on to the NetHandler for processing.
    */
   public void handle(ServerLoginPacketListener pHandler) {
      pHandler.handleCustomQueryPacket(this);
   }

   @Nullable @Override public FriendlyByteBuf getInternalData() { return this.payload instanceof DiscardedQueryAnswerPayload dc ? dc.data() : null; }
   @Override public net.minecraft.resources.ResourceLocation getName() { return net.minecraftforge.network.NetworkInitialization.LOGIN_NAME; }
   @Override public int getIndex() { return transactionId(); }
}
