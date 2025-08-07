package cuspymd.mcp.mod.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.*;

public class BlockCompressor {
    
    public static class BlockRegion {
        public final int startX, startY, startZ;
        public final int endX, endY, endZ;
        
        public BlockRegion(int startX, int startY, int startZ, int endX, int endY, int endZ) {
            this.startX = Math.min(startX, endX);
            this.startY = Math.min(startY, endY);
            this.startZ = Math.min(startZ, endZ);
            this.endX = Math.max(startX, endX);
            this.endY = Math.max(startY, endY);
            this.endZ = Math.max(startZ, endZ);
        }
        
        public boolean isSingleBlock() {
            return startX == endX && startY == endY && startZ == endZ;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof BlockRegion)) return false;
            BlockRegion other = (BlockRegion) obj;
            return startX == other.startX && startY == other.startY && startZ == other.startZ &&
                   endX == other.endX && endY == other.endY && endZ == other.endZ;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(startX, startY, startZ, endX, endY, endZ);
        }
    }
    
    public static class BlockPosition {
        public final int x, y, z;
        
        public BlockPosition(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof BlockPosition)) return false;
            BlockPosition other = (BlockPosition) obj;
            return x == other.x && y == other.y && z == other.z;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(x, y, z);
        }
    }
    
    public static JsonObject compressBlocks(List<JsonObject> blockList) {
        Map<String, Set<BlockPosition>> blocksByType = new HashMap<>();
        
        // Group blocks by type
        for (JsonObject block : blockList) {
            String blockType = block.get("type").getAsString();
            int x = block.get("x").getAsInt();
            int y = block.get("y").getAsInt();
            int z = block.get("z").getAsInt();
            
            blocksByType.computeIfAbsent(blockType, k -> new HashSet<>())
                       .add(new BlockPosition(x, y, z));
        }
        
        JsonObject result = new JsonObject();
        JsonArray compressedBlocks = new JsonArray();
        
        for (Map.Entry<String, Set<BlockPosition>> entry : blocksByType.entrySet()) {
            String blockType = entry.getKey();
            Set<BlockPosition> positions = entry.getValue();
            
            // Find connected regions
            Map<BlockPosition, Boolean> visited = new HashMap<>();
            positions.forEach(pos -> visited.put(pos, false));
            
            List<BlockPosition> singleBlocks = new ArrayList<>();
            List<BlockRegion> regions = new ArrayList<>();
            
            for (BlockPosition pos : positions) {
                if (!visited.get(pos)) {
                    Set<BlockPosition> connectedComponent = findConnectedComponent(pos, positions, visited);
                    
                    if (connectedComponent.size() == 1) {
                        singleBlocks.add(pos);
                    } else {
                        BlockRegion region = calculateBoundingBox(connectedComponent);
                        regions.add(region);
                    }
                }
            }
            
            // Create JSON for this block type
            JsonObject blockTypeObj = new JsonObject();
            blockTypeObj.addProperty("blockType", blockType);
            
            if (!singleBlocks.isEmpty()) {
                JsonArray singleBlocksArray = new JsonArray();
                for (BlockPosition pos : singleBlocks) {
                    JsonObject posObj = new JsonObject();
                    posObj.addProperty("x", pos.x);
                    posObj.addProperty("y", pos.y);
                    posObj.addProperty("z", pos.z);
                    singleBlocksArray.add(posObj);
                }
                blockTypeObj.add("singleBlocks", singleBlocksArray);
            }
            
            if (!regions.isEmpty()) {
                JsonArray regionsArray = new JsonArray();
                for (BlockRegion region : regions) {
                    JsonObject regionObj = new JsonObject();
                    
                    JsonObject startObj = new JsonObject();
                    startObj.addProperty("x", region.startX);
                    startObj.addProperty("y", region.startY);
                    startObj.addProperty("z", region.startZ);
                    
                    JsonObject endObj = new JsonObject();
                    endObj.addProperty("x", region.endX);
                    endObj.addProperty("y", region.endY);
                    endObj.addProperty("z", region.endZ);
                    
                    regionObj.add("start", startObj);
                    regionObj.add("end", endObj);
                    regionsArray.add(regionObj);
                }
                blockTypeObj.add("regions", regionsArray);
            }
            
            compressedBlocks.add(blockTypeObj);
        }
        
        result.add("blocks", compressedBlocks);
        return result;
    }
    
    private static Set<BlockPosition> findConnectedComponent(
            BlockPosition start, 
            Set<BlockPosition> allPositions, 
            Map<BlockPosition, Boolean> visited) {
        
        Set<BlockPosition> component = new HashSet<>();
        Queue<BlockPosition> queue = new LinkedList<>();
        
        queue.offer(start);
        visited.put(start, true);
        
        while (!queue.isEmpty()) {
            BlockPosition current = queue.poll();
            component.add(current);
            
            // Check 6 adjacent positions (up, down, north, south, east, west)
            int[][] directions = {
                {0, 1, 0}, {0, -1, 0},  // up, down
                {1, 0, 0}, {-1, 0, 0}, // east, west
                {0, 0, 1}, {0, 0, -1}  // south, north
            };
            
            for (int[] dir : directions) {
                BlockPosition neighbor = new BlockPosition(
                    current.x + dir[0], 
                    current.y + dir[1], 
                    current.z + dir[2]
                );
                
                if (allPositions.contains(neighbor) && !visited.get(neighbor)) {
                    visited.put(neighbor, true);
                    queue.offer(neighbor);
                }
            }
        }
        
        return component;
    }
    
    private static BlockRegion calculateBoundingBox(Set<BlockPosition> positions) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        
        for (BlockPosition pos : positions) {
            minX = Math.min(minX, pos.x);
            minY = Math.min(minY, pos.y);
            minZ = Math.min(minZ, pos.z);
            maxX = Math.max(maxX, pos.x);
            maxY = Math.max(maxY, pos.y);
            maxZ = Math.max(maxZ, pos.z);
        }
        
        return new BlockRegion(minX, minY, minZ, maxX, maxY, maxZ);
    }
}