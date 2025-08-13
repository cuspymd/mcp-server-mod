package cuspymd.mcp.mod.utils;

import com.google.gson.JsonObject;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;

public class BlockScanner {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlockScanner.class);
    
    public static JsonObject scanBlocksInArea(JsonObject fromPos, JsonObject toPos, int maxAreaSize) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world == null) {
                return createErrorResponse("World not available");
            }
            
            World world = client.world;
            
            // Parse coordinates
            int fromX = fromPos.get("x").getAsInt();
            int fromY = fromPos.get("y").getAsInt();  
            int fromZ = fromPos.get("z").getAsInt();
            
            int toX = toPos.get("x").getAsInt();
            int toY = toPos.get("y").getAsInt();
            int toZ = toPos.get("z").getAsInt();
            
            // Ensure from coordinates are smaller than to coordinates
            int minX = Math.min(fromX, toX);
            int maxX = Math.max(fromX, toX);
            int minY = Math.min(fromY, toY);
            int maxY = Math.max(fromY, toY);
            int minZ = Math.min(fromZ, toZ);
            int maxZ = Math.max(fromZ, toZ);
            
            // Check area size limits
            int sizeX = maxX - minX + 1;
            int sizeY = maxY - minY + 1;
            int sizeZ = maxZ - minZ + 1;
            
            if (sizeX > maxAreaSize || sizeY > maxAreaSize || sizeZ > maxAreaSize) {
                return createErrorResponse(String.format(
                    "Area too large. Maximum size per axis: %d blocks. Requested: %dx%dx%d", 
                    maxAreaSize, sizeX, sizeY, sizeZ
                ));
            }
            
            JsonObject result = new JsonObject();
            
            // Add area info
            JsonObject areaInfo = new JsonObject();
            JsonObject fromCoords = new JsonObject();
            fromCoords.addProperty("x", minX);
            fromCoords.addProperty("y", minY);
            fromCoords.addProperty("z", minZ);
            
            JsonObject toCoords = new JsonObject();
            toCoords.addProperty("x", maxX);
            toCoords.addProperty("y", maxY);  
            toCoords.addProperty("z", maxZ);
            
            areaInfo.add("from", fromCoords);
            areaInfo.add("to", toCoords);
            areaInfo.addProperty("size", String.format("%dx%dx%d", sizeX, sizeY, sizeZ));
            result.add("area", areaInfo);
            
            // Scan blocks
            List<JsonObject> blockList = new ArrayList<>();
            int totalBlocks = 0;
            
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        var blockState = world.getBlockState(pos);
                        
                        // Skip air blocks
                        if (blockState.isOf(Blocks.AIR) || blockState.isOf(Blocks.VOID_AIR) || blockState.isOf(Blocks.CAVE_AIR)) {
                            continue;
                        }
                        
                        JsonObject blockInfo = new JsonObject();
                        blockInfo.addProperty("x", x);
                        blockInfo.addProperty("y", y);
                        blockInfo.addProperty("z", z);
                        blockInfo.addProperty("type", blockState.getBlock().toString());
                        
                        blockList.add(blockInfo);
                        totalBlocks++;
                    }
                }
            }
            
            // Compress blocks using BlockCompressor
            JsonObject compressedBlocks = BlockCompressor.compressBlocks(blockList);
            
            result.addProperty("total_blocks", totalBlocks);
            result.add("blocks", compressedBlocks.get("blocks"));
            
            LOGGER.info("Scanned area {}x{}x{}, found {} non-air blocks", sizeX, sizeY, sizeZ, totalBlocks);
            
            return result;
            
        } catch (Exception e) {
            LOGGER.error("Error scanning blocks in area", e);
            return createErrorResponse("Failed to scan blocks: " + e.getMessage());
        }
    }
    
    private static JsonObject createErrorResponse(String message) {
        JsonObject error = new JsonObject();
        error.addProperty("error", message);
        return error;
    }
}