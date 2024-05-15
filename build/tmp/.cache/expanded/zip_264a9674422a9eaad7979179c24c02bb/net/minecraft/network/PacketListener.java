package net.minecraft.network;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;

/**
 * Describes how packets are handled. There are various implementations of this class for each possible protocol (e.g.
 * PLAY, CLIENTBOUND; PLAY, SERVERBOUND; etc.)
 */
public interface PacketListener {
   PacketFlow flow();

   ConnectionProtocol protocol();

   /**
    * Invoked when disconnecting, the parameter is a ChatComponent describing the reason for termination
    */
   void onDisconnect(Component pReason);

   boolean isAcceptingMessages();

   default boolean shouldHandleMessage(Packet<?> pPacket) {
      return this.isAcceptingMessages();
   }

   default boolean shouldPropagateHandlingExceptions() {
      return true;
   }
}