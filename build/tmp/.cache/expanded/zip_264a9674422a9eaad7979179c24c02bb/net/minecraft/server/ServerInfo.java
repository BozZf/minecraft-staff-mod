package net.minecraft.server;

public interface ServerInfo {
   String getMotd();

   /**
    * Returns the server's Minecraft version as string.
    */
   String getServerVersion();

   /**
    * Returns the number of players currently on the server.
    */
   int getPlayerCount();

   /**
    * Returns the maximum number of players allowed on the server.
    */
   int getMaxPlayers();
}