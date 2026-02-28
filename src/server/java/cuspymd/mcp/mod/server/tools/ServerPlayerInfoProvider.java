package cuspymd.mcp.mod.server.tools;

import com.google.gson.JsonObject;
import cuspymd.mcp.mod.utils.IPlayerInfoProvider;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

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
                if (server == null || server.getPlayerManager() == null || server.getPlayerManager().getPlayerList().isEmpty()) {
                    info.addProperty("error", "No players online on the server");
                    return info;
                }

                // Just take the first player for now
                ServerPlayerEntity player = server.getPlayerManager().getPlayerList().get(0);

                JsonObject pos = new JsonObject();
                pos.addProperty("x", player.getX());
                pos.addProperty("y", player.getY());
                pos.addProperty("z", player.getZ());
                info.add("position", pos);

                JsonObject blockPos = new JsonObject();
                blockPos.addProperty("x", player.getBlockPos().getX());
                blockPos.addProperty("y", player.getBlockPos().getY());
                blockPos.addProperty("z", player.getBlockPos().getZ());
                info.add("blockPosition", blockPos);

                info.addProperty("dimension", player.getWorld().getRegistryKey().getValue().toString());
                info.addProperty("gameMode", player.interactionManager.getGameMode().getName());
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