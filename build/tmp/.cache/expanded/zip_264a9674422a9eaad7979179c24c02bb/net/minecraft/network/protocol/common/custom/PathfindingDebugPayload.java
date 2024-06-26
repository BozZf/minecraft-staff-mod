package net.minecraft.network.protocol.common.custom;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.pathfinder.Path;

public record PathfindingDebugPayload(int entityId, Path path, float maxNodeDistance) implements CustomPacketPayload {
   public static final ResourceLocation ID = new ResourceLocation("debug/path");

   public PathfindingDebugPayload(FriendlyByteBuf pBuffer) {
      this(pBuffer.readInt(), Path.createFromStream(pBuffer), pBuffer.readFloat());
   }

   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeInt(this.entityId);
      this.path.writeToStream(pBuffer);
      pBuffer.writeFloat(this.maxNodeDistance);
   }

   public ResourceLocation id() {
      return ID;
   }
}