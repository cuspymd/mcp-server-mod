package cuspymd.mcp.mod;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import cuspymd.mcp.mod.utils.BlockCompressor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BlockCompressorTest {

    private JsonObject createBlock(int x, int y, int z, String type) {
        JsonObject block = new JsonObject();
        block.addProperty("x", x);
        block.addProperty("y", y);
        block.addProperty("z", z);
        block.addProperty("type", type);
        return block;
    }

    @Test
    public void testSingleBlockCompression() {
        List<JsonObject> blocks = new ArrayList<>();
        blocks.add(createBlock(10, 64, 10, "minecraft:stone"));
        
        JsonObject result = BlockCompressor.compressBlocks(blocks);
        JsonArray blockTypes = result.getAsJsonArray("blocks");
        
        assertEquals(1, blockTypes.size());
        JsonObject stoneBlocks = blockTypes.get(0).getAsJsonObject();
        assertEquals("minecraft:stone", stoneBlocks.get("blockType").getAsString());
        
        assertTrue(stoneBlocks.has("singleBlocks"));
        assertFalse(stoneBlocks.has("regions"));
        
        JsonArray singleBlocks = stoneBlocks.getAsJsonArray("singleBlocks");
        assertEquals(1, singleBlocks.size());
        
        JsonObject singleBlock = singleBlocks.get(0).getAsJsonObject();
        assertEquals(10, singleBlock.get("x").getAsInt());
        assertEquals(64, singleBlock.get("y").getAsInt());
        assertEquals(10, singleBlock.get("z").getAsInt());
    }

    @Test
    public void testConnectedBlocksCompression() {
        List<JsonObject> blocks = new ArrayList<>();
        // Create a 2x2x2 cube of stone blocks
        for (int x = 10; x <= 11; x++) {
            for (int y = 64; y <= 65; y++) {
                for (int z = 10; z <= 11; z++) {
                    blocks.add(createBlock(x, y, z, "minecraft:stone"));
                }
            }
        }
        
        JsonObject result = BlockCompressor.compressBlocks(blocks);
        JsonArray blockTypes = result.getAsJsonArray("blocks");
        
        assertEquals(1, blockTypes.size());
        JsonObject stoneBlocks = blockTypes.get(0).getAsJsonObject();
        assertEquals("minecraft:stone", stoneBlocks.get("blockType").getAsString());
        
        assertTrue(stoneBlocks.has("regions"));
        assertFalse(stoneBlocks.has("singleBlocks"));
        
        JsonArray regions = stoneBlocks.getAsJsonArray("regions");
        assertEquals(1, regions.size());
        
        JsonObject region = regions.get(0).getAsJsonObject();
        JsonObject start = region.getAsJsonObject("start");
        JsonObject end = region.getAsJsonObject("end");
        
        assertEquals(10, start.get("x").getAsInt());
        assertEquals(64, start.get("y").getAsInt());
        assertEquals(10, start.get("z").getAsInt());
        
        assertEquals(11, end.get("x").getAsInt());
        assertEquals(65, end.get("y").getAsInt());
        assertEquals(11, end.get("z").getAsInt());
    }

    @Test
    public void testMixedBlocksCompression() {
        List<JsonObject> blocks = new ArrayList<>();
        
        // Single stone block
        blocks.add(createBlock(0, 64, 0, "minecraft:stone"));
        
        // Connected dirt blocks (line of 3)
        blocks.add(createBlock(10, 64, 10, "minecraft:dirt"));
        blocks.add(createBlock(11, 64, 10, "minecraft:dirt"));
        blocks.add(createBlock(12, 64, 10, "minecraft:dirt"));
        
        // Scattered grass blocks
        blocks.add(createBlock(20, 64, 20, "minecraft:grass_block"));
        blocks.add(createBlock(25, 64, 25, "minecraft:grass_block"));
        
        JsonObject result = BlockCompressor.compressBlocks(blocks);
        JsonArray blockTypes = result.getAsJsonArray("blocks");
        
        assertEquals(3, blockTypes.size());
        
        // Find each block type
        JsonObject stoneBlocks = null, dirtBlocks = null, grassBlocks = null;
        for (int i = 0; i < blockTypes.size(); i++) {
            JsonObject blockType = blockTypes.get(i).getAsJsonObject();
            String type = blockType.get("blockType").getAsString();
            switch (type) {
                case "minecraft:stone":
                    stoneBlocks = blockType;
                    break;
                case "minecraft:dirt":
                    dirtBlocks = blockType;
                    break;
                case "minecraft:grass_block":
                    grassBlocks = blockType;
                    break;
            }
        }
        
        // Test stone (single block)
        assertNotNull(stoneBlocks);
        assertTrue(stoneBlocks.has("singleBlocks"));
        assertFalse(stoneBlocks.has("regions"));
        assertEquals(1, stoneBlocks.getAsJsonArray("singleBlocks").size());
        
        // Test dirt (connected region)
        assertNotNull(dirtBlocks);
        assertFalse(dirtBlocks.has("singleBlocks"));
        assertTrue(dirtBlocks.has("regions"));
        assertEquals(1, dirtBlocks.getAsJsonArray("regions").size());
        
        JsonObject dirtRegion = dirtBlocks.getAsJsonArray("regions").get(0).getAsJsonObject();
        JsonObject dirtStart = dirtRegion.getAsJsonObject("start");
        JsonObject dirtEnd = dirtRegion.getAsJsonObject("end");
        assertEquals(10, dirtStart.get("x").getAsInt());
        assertEquals(12, dirtEnd.get("x").getAsInt());
        
        // Test grass (multiple single blocks)
        assertNotNull(grassBlocks);
        assertTrue(grassBlocks.has("singleBlocks"));
        assertFalse(grassBlocks.has("regions"));
        assertEquals(2, grassBlocks.getAsJsonArray("singleBlocks").size());
    }

    @Test
    public void testDisconnectedBlocksOfSameType() {
        List<JsonObject> blocks = new ArrayList<>();
        
        // Two separate stone blocks
        blocks.add(createBlock(0, 64, 0, "minecraft:stone"));
        blocks.add(createBlock(10, 64, 10, "minecraft:stone"));
        
        JsonObject result = BlockCompressor.compressBlocks(blocks);
        JsonArray blockTypes = result.getAsJsonArray("blocks");
        
        assertEquals(1, blockTypes.size());
        JsonObject stoneBlocks = blockTypes.get(0).getAsJsonObject();
        
        assertTrue(stoneBlocks.has("singleBlocks"));
        assertFalse(stoneBlocks.has("regions"));
        
        JsonArray singleBlocks = stoneBlocks.getAsJsonArray("singleBlocks");
        assertEquals(2, singleBlocks.size());
    }

    @Test
    public void testEmptyBlockList() {
        List<JsonObject> blocks = new ArrayList<>();
        
        JsonObject result = BlockCompressor.compressBlocks(blocks);
        JsonArray blockTypes = result.getAsJsonArray("blocks");
        
        assertEquals(0, blockTypes.size());
    }

    @Test
    public void testLShapedConnectedBlocks() {
        List<JsonObject> blocks = new ArrayList<>();
        
        // Create an L-shaped structure
        blocks.add(createBlock(10, 64, 10, "minecraft:stone"));
        blocks.add(createBlock(11, 64, 10, "minecraft:stone"));
        blocks.add(createBlock(12, 64, 10, "minecraft:stone"));
        blocks.add(createBlock(10, 64, 11, "minecraft:stone"));
        blocks.add(createBlock(10, 64, 12, "minecraft:stone"));
        
        JsonObject result = BlockCompressor.compressBlocks(blocks);
        JsonArray blockTypes = result.getAsJsonArray("blocks");
        
        assertEquals(1, blockTypes.size());
        JsonObject stoneBlocks = blockTypes.get(0).getAsJsonObject();
        
        // L-shaped blocks should be treated as a region since they're connected
        assertFalse(stoneBlocks.has("singleBlocks"));
        assertTrue(stoneBlocks.has("regions"));
        
        JsonArray regions = stoneBlocks.getAsJsonArray("regions");
        assertEquals(1, regions.size());
        
        JsonObject region = regions.get(0).getAsJsonObject();
        JsonObject start = region.getAsJsonObject("start");
        JsonObject end = region.getAsJsonObject("end");
        
        // Bounding box should encompass the entire L-shape
        assertEquals(10, start.get("x").getAsInt());
        assertEquals(10, start.get("z").getAsInt());
        assertEquals(12, end.get("x").getAsInt());
        assertEquals(12, end.get("z").getAsInt());
    }

    @Test
    public void testVerticallyConnectedBlocks() {
        List<JsonObject> blocks = new ArrayList<>();
        
        // Create a vertical tower
        for (int y = 64; y <= 67; y++) {
            blocks.add(createBlock(10, y, 10, "minecraft:stone"));
        }
        
        JsonObject result = BlockCompressor.compressBlocks(blocks);
        JsonArray blockTypes = result.getAsJsonArray("blocks");
        
        assertEquals(1, blockTypes.size());
        JsonObject stoneBlocks = blockTypes.get(0).getAsJsonObject();
        
        assertFalse(stoneBlocks.has("singleBlocks"));
        assertTrue(stoneBlocks.has("regions"));
        
        JsonArray regions = stoneBlocks.getAsJsonArray("regions");
        assertEquals(1, regions.size());
        
        JsonObject region = regions.get(0).getAsJsonObject();
        JsonObject start = region.getAsJsonObject("start");
        JsonObject end = region.getAsJsonObject("end");
        
        assertEquals(10, start.get("x").getAsInt());
        assertEquals(64, start.get("y").getAsInt());
        assertEquals(10, start.get("z").getAsInt());
        
        assertEquals(10, end.get("x").getAsInt());
        assertEquals(67, end.get("y").getAsInt());
        assertEquals(10, end.get("z").getAsInt());
    }
}