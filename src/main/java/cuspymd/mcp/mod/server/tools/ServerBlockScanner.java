package cuspymd.mcp.mod.server.tools;

import com.google.gson.JsonObject;
import cuspymd.mcp.mod.utils.IBlockScanner;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class ServerBlockScanner implements IBlockScanner {
    private final MinecraftServer server;

    public ServerBlockScanner(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public JsonObject scanBlocksInArea(JsonObject fromPos, JsonObject toPos, int maxAreaSize) {
        try {
            return server.submit(() -> {
                if (server == null) {
                    JsonObject error = new JsonObject();
                    error.addProperty("error", "Server instance not available");
                    return error;
                }

                ServerWorld world;
                if (server.getPlayerManager() == null || server.getPlayerManager().getPlayerList().isEmpty()) {
                    // Fallback to the overworld if no players are online
                    world = server.getOverworld();
                } else {
                    // Use the first player's world
                    ServerPlayerEntity player = server.getPlayerManager().getPlayerList().get(0);
                    world = player.getCommandSource().getWorld();
                }

                int x1 = fromPos.get("x").getAsInt();
                int y1 = fromPos.get("y").getAsInt();
                int z1 = fromPos.get("z").getAsInt();
                int x2 = toPos.get("x").getAsInt();
                int y2 = toPos.get("y").getAsInt();
                int z2 = toPos.get("z").getAsInt();

                int minX = Math.min(x1, x2);
                int minY = Math.min(y1, y2);
                int minZ = Math.min(z1, z2);
                int maxX = Math.max(x1, x2);
                int maxY = Math.max(y1, y2);
                int maxZ = Math.max(z1, z2);

                int dx = maxX - minX + 1;
                int dy = maxY - minY + 1;
                int dz = maxZ - minZ + 1;

                if (dx > maxAreaSize || dy > maxAreaSize || dz > maxAreaSize) {
                    JsonObject error = new JsonObject();
                    error.addProperty("error", "Area too large. Max size is " + maxAreaSize + " per axis.");
                    return error;
                }

                java.util.List<cuspymd.mcp.mod.utils.BlockCompressor.BlockData> blocks = new java.util.ArrayList<>();
                int count = 0;

                for (int x = minX; x <= maxX; x++) {
                    for (int y = minY; y <= maxY; y++) {
                        for (int z = minZ; z <= maxZ; z++) {
                            BlockPos pos = new BlockPos(x, y, z);
                            net.minecraft.block.BlockState state = world.getBlockState(pos);

                            if (!state.isAir()) {
                                String name = net.minecraft.registry.Registries.BLOCK.getId(state.getBlock()).toString();
                                blocks.add(new cuspymd.mcp.mod.utils.BlockCompressor.BlockData(x, y, z, name));
                                count++;
                            }
                        }
                    }
                }

                JsonObject result = cuspymd.mcp.mod.utils.BlockCompressor.compressBlocks(blocks);

                JsonObject stats = new JsonObject();
                stats.addProperty("total_scanned", dx * dy * dz);
                stats.addProperty("non_air_blocks", count);
                result.add("stats", stats);

                return result;
            }).get();
        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("error", "Failed to scan blocks: " + e.getMessage());
            return error;
        }
    }
}