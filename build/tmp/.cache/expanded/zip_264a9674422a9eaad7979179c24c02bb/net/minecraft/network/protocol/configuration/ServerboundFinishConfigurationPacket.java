package net.minecraft.network.protocol.configuration;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public record ServerboundFinishConfigurationPacket() implements Packet<ServerConfigurationPacketListener> {
   public ServerboundFinishConfigurationPacket(FriendlyByteBuf p_298790_) {
      this();
   }

   /**
    * Writes the raw packet data to the data stream.
    */
   public void write(FriendlyByteBuf p_300295_) {
   }

   /**
    * Passes this Packet on to the NetHandler for processing.
    */
   public void handle(ServerConfigurationPacketListener p_299852_) {
      p_299852_.handleConfigurationFinished(this);
   }

   public ConnectionProtocol nextProtocol() {
      return ConnectionProtocol.PLAY;
   }
}