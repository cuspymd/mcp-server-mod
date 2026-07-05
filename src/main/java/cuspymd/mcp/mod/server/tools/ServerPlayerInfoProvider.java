package cuspymd.mcp.mod.server.tools;

import com.google.gson.JsonObject;
import cuspymd.mcp.mod.utils.IPlayerInfoProvider;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class ServerPlayerInfoProvider implements IPlayerInfoProvider {
    private final MinecraftServer server;

    public ServerPlayerInfoProvider(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public JsonObject getPlayerInfo() {
        try {
            return server.submit(() -> {
                JsonObject info = new JsonObject();
                if (server == null || server.getPlayerList() == null || server.getPlayerList().getPlayers().isEmpty()) {
                    info.addProperty("error", "No players online on the server");
                    return info;
                }

                // Just take the first player for now
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);

                JsonObject pos = new JsonObject();
                pos.addProperty("x", player.getX());
                pos.addProperty("y", player.getY());
                pos.addProperty("z", player.getZ());
                info.add("position", pos);

                JsonObject blockPos = new JsonObject();
                blockPos.addProperty("x", player.blockPosition().getX());
                blockPos.addProperty("y", player.blockPosition().getY());
                blockPos.addProperty("z", player.blockPosition().getZ());
                info.add("blockPosition", blockPos);

                info.addProperty("dimension", player.createCommandSourceStack().getLevel().dimension().identifier().toString());
                info.addProperty("gameMode", player.gameMode.getGameModeForPlayer().name().toLowerCase());
                info.addProperty("health", player.getHealth());

                return info;
            }).get();
        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("error", "Failed to retrieve player info: " + e.getMessage());
            return error;
        }
    }
}