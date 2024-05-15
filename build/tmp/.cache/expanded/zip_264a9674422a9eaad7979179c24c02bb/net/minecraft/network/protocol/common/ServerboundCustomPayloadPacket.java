package net.minecraft.network.protocol.common;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.resources.ResourceLocation;

public record ServerboundCustomPayloadPacket(CustomPacketPayload payload) implements Packet<ServerCommonPacketListener>, net.minecraftforge.network.ICustomPacket<ServerboundCustomPayloadPacket> {
   private static final int MAX_PAYLOAD_SIZE = 32767;
   private static final Map<ResourceLocation, FriendlyByteBuf.Reader<? extends CustomPacketPayload>> KNOWN_TYPES = ImmutableMap.<ResourceLocation, FriendlyByteBuf.Reader<? extends CustomPacketPayload>>builder().put(BrandPayload.ID, BrandPayload::new).build();

   public ServerboundCustomPayloadPacket(FriendlyByteBuf pBuffer) {
      this(readPayload(pBuffer.readResourceLocation(), pBuffer));
   }

   private static CustomPacketPayload readPayload(ResourceLocation pId, FriendlyByteBuf pBuffer) {
      FriendlyByteBuf.Reader<? extends CustomPacketPayload> reader = KNOWN_TYPES.get(pId);
      return (CustomPacketPayload)(reader != null ? reader.apply(pBuffer) : readUnknownPayload(pId, pBuffer));
   }

   private static DiscardedPayload readUnknownPayload(ResourceLocation pId, FriendlyByteBuf pBuffer) {
      int i = pBuffer.readableBytes();
      if (i >= 0 && i <= 32767) {
         return new DiscardedPayload(pId, new FriendlyByteBuf(pBuffer.readBytes(i)));
      } else {
         throw new IllegalArgumentException("Payload may not be larger than 32767 bytes");
      }
   }

   /**
    * Writes the raw packet data to the data stream.
    */
   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeResourceLocation(this.payload.id());
      this.payload.write(pBuffer);
   }

   /**
    * Passes this Packet on to the NetHandler for processing.
    */
   public void handle(ServerCommonPacketListener pHandler) {
      pHandler.handleCustomPayload(this);
   }

   @Override public @org.jetbrains.annotations.Nullable FriendlyByteBuf getInternalData() { return payload instanceof DiscardedPayload dc ? dc.data() : null; }
   @Override public ResourceLocation getName() { return payload.id(); }
   @Override public int getIndex() { return 0; }
}
