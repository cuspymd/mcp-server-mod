package cuspymd.mcp.mod.utils;

import net.minecraft.util.math.BlockPos;

public class CoordinateUtils {
    
    public static class BoundingBox {
        public final int minX, minY, minZ;
        public final int maxX, maxY, maxZ;
        
        public BoundingBox(int x1, int y1, int z1, int x2, int y2, int z2) {
            this.minX = Math.min(x1, x2);
            this.minY = Math.min(y1, y2);
            this.minZ = Math.min(z1, z2);
            this.maxX = Math.max(x1, x2);
            this.maxY = Math.max(y1, y2);
            this.maxZ = Math.max(z1, z2);
        }
        
        public long getVolume() {
            return (long)(maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        }
        
        public int getWidth() { return maxX - minX + 1; }
        public int getHeight() { return maxY - minY + 1; }
        public int getDepth() { return maxZ - minZ + 1; }
    }
    
    public static BoundingBox parseBoundingBox(String x1, String y1, String z1, String x2, String y2, String z2) {
        try {
            return new BoundingBox(
                Integer.parseInt(x1), Integer.parseInt(y1), Integer.parseInt(z1),
                Integer.parseInt(x2), Integer.parseInt(y2), Integer.parseInt(z2)
            );
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid coordinate format", e);
        }
    }
    
    public static boolean isRelativeCoordinate(String coord) {
        return coord.startsWith("~") || coord.startsWith("^");
    }
    
    public static String formatCoordinate(BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }
    
    public static BlockPos parseBlockPos(String x, String y, String z) {
        try {
            return new BlockPos(
                Integer.parseInt(x.replace("~", "0")),
                Integer.parseInt(y.replace("~", "0")),
                Integer.parseInt(z.replace("~", "0"))
            );
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid block position format", e);
        }
    }
    
    public static double calculateDistance(BlockPos pos1, BlockPos pos2) {
        double dx = pos1.getX() - pos2.getX();
        double dy = pos1.getY() - pos2.getY();
        double dz = pos1.getZ() - pos2.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}