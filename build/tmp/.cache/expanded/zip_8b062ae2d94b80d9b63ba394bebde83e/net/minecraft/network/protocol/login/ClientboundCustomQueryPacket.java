package net.minecraft.network.protocol.login;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.login.custom.CustomQueryPayload;
import net.minecraft.network.protocol.login.custom.DiscardedQueryPayload;
import net.minecraft.resources.ResourceLocation;

public record ClientboundCustomQueryPacket(int transactionId, CustomQueryPayload payload) implements Packet<ClientLoginPacketListener>, net.minecraftforge.network.ICustomPacket<ClientboundCustomQueryPacket> {
   private static final int MAX_PAYLOAD_SIZE = 1048576;

   public ClientboundCustomQueryPacket(FriendlyByteBuf p_179810_) {
      this(p_179810_.readVarInt(), readPayload(p_179810_.readResourceLocation(), p_179810_));
   }

   private static CustomQueryPayload readPayload(ResourceLocation p_300079_, FriendlyByteBuf p_299332_) {
      return readUnknownPayload(p_300079_, p_299332_);
   }

   private static DiscardedQueryPayload readUnknownPayload(ResourceLocation p_299981_, FriendlyByteBuf p_297706_) {
      int i = p_297706_.readableBytes();
      if (i >= 0 && i <= 1048576) {
         return new DiscardedQueryPayload(p_299981_, new FriendlyByteBuf(p_297706_.readBytes(i)));
      } else {
         throw new IllegalArgumentException("Payload may not be larger than 1048576 bytes");
      }
   }

   public void write(FriendlyByteBuf p_134757_) {
      p_134757_.writeVarInt(this.transactionId);
      p_134757_.writeResourceLocation(this.payload.id());
      this.payload.write(p_134757_);
   }

   public void handle(ClientLoginPacketListener p_134754_) {
      p_134754_.handleCustomQuery(this);
   }

   @Override public int getIndex() { return transactionId(); }
   @Override public ResourceLocation getName() { return this.payload.id(); }
   @org.jetbrains.annotations.Nullable @Override public FriendlyByteBuf getInternalData() { return ((DiscardedQueryPayload)this.payload).data(); }
}
