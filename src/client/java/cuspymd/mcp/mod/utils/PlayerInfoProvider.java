package cuspymd.mcp.mod.utils;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class PlayerInfoProvider implements cuspymd.mcp.mod.utils.IPlayerInfoProvider {
    
    @Override
    public JsonObject getPlayerInfo() {
        return getPlayerInfoStatic();
    }

    public static JsonObject getPlayerInfoStatic() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        Level world = client.level;
        
        JsonObject playerInfo = new JsonObject();
        
        if (player == null) {
            playerInfo.addProperty("error", "No player found");
            return playerInfo;
        }
        
        // Position information
        JsonObject position = new JsonObject();
        position.addProperty("x", player.getX());
        position.addProperty("y", player.getY());
        position.addProperty("z", player.getZ());
        playerInfo.add("position", position);
        
        // Block position (for commands that need integer coordinates)
        JsonObject blockPos = new JsonObject();
        blockPos.addProperty("x", player.getBlockX());
        blockPos.addProperty("y", player.getBlockY());
        blockPos.addProperty("z", player.getBlockZ());
        playerInfo.add("blockPosition", blockPos);
        
        // Facing direction
        JsonObject rotation = new JsonObject();
        rotation.addProperty("yaw", player.getYRot());
        rotation.addProperty("pitch", player.getXRot());
        playerInfo.add("rotation", rotation);
        
        // Cardinal direction (N, S, E, W, NE, NW, SE, SW)
        String direction = getCardinalDirection(player.getYRot());
        playerInfo.addProperty("facingDirection", direction);
        
        // Look vector (normalized direction the player is looking)
        Vec3 lookVec = player.getViewVector(1.0f);
        JsonObject lookVector = new JsonObject();
        lookVector.addProperty("x", lookVec.x);
        lookVector.addProperty("y", lookVec.y);
        lookVector.addProperty("z", lookVec.z);
        playerInfo.add("lookVector", lookVector);
        
        // Calculate position in front of player (useful for building).
        // Only the horizontal (x/z) look direction is used; pitch is ignored so that
        // looking slightly up/down doesn't shift the reported ground level by a block
        // (lookVec.y is non-zero for almost any pitch, which previously made Math.floor
        // round frontPosition.y down below the player's actual standing height).
        double horizontalLength = Math.sqrt(lookVec.x * lookVec.x + lookVec.z * lookVec.z);
        double dirX;
        double dirZ;
        if (horizontalLength > 1e-6) {
            dirX = lookVec.x / horizontalLength;
            dirZ = lookVec.z / horizontalLength;
        } else {
            // Looking (near) straight up/down collapses the horizontal look vector to
            // zero, even though yaw still points somewhere meaningful. Fall back to
            // yaw alone so frontPosition stays 3 blocks ahead instead of on top of the player.
            float yawRad = player.getYRot() * ((float) Math.PI / 180F);
            dirX = -Math.sin(yawRad);
            dirZ = Math.cos(yawRad);
        }
        Vec3 playerPos = new Vec3(player.getX(), player.getY(), player.getZ());
        Vec3 frontPos = new Vec3(playerPos.x + dirX * 3.0, playerPos.y, playerPos.z + dirZ * 3.0);
        JsonObject frontPosition = new JsonObject();
        frontPosition.addProperty("x", (int) Math.floor(frontPos.x));
        frontPosition.addProperty("y", (int) Math.floor(frontPos.y));
        frontPosition.addProperty("z", (int) Math.floor(frontPos.z));
        playerInfo.add("frontPosition", frontPosition);
        
        // Health and food information
        playerInfo.addProperty("health", player.getHealth());
        playerInfo.addProperty("maxHealth", player.getMaxHealth());
        playerInfo.addProperty("foodLevel", player.getFoodData().getFoodLevel());
        playerInfo.addProperty("saturation", player.getFoodData().getSaturationLevel());
        
        // Game mode
        if (client.gameMode != null) {
            playerInfo.addProperty("gameMode", client.gameMode.getPlayerMode().name().toLowerCase());
        }
        
        // Dimension information
        if (world != null) {
            playerInfo.addProperty("dimension", world.dimension().identifier().toString());
            playerInfo.addProperty("timeOfDay", world.getDefaultClockTime());
            playerInfo.addProperty("isDay", world.isBrightOutside());
            playerInfo.addProperty("isNight", world.isDarkOutside());
        }
        
        // Player name
        playerInfo.addProperty("name", player.getName().getString());
        
        // Experience information
        playerInfo.addProperty("experienceLevel", player.experienceLevel);
        playerInfo.addProperty("experienceProgress", player.experienceProgress);
        playerInfo.addProperty("totalExperience", player.totalExperience);
        
        // Inventory information
        JsonObject inventory = new JsonObject();
        inventory.addProperty("selectedSlot", player.getInventory().getSelectedSlot());
        inventory.addProperty("mainHandItem", player.getMainHandItem().isEmpty() ? "empty" : 
            player.getMainHandItem().getItem().toString());
        inventory.addProperty("offHandItem", player.getOffhandItem().isEmpty() ? "empty" : 
            player.getOffhandItem().getItem().toString());
        playerInfo.add("inventory", inventory);
        
        return playerInfo;
    }
    
    private static String getCardinalDirection(float yaw) {
        // Normalize yaw to 0-360 range
        yaw = yaw % 360;
        if (yaw < 0) {
            yaw += 360;
        }
        
        // Convert yaw to cardinal direction
        // In Minecraft, yaw 0 is South, 90 is West, 180 is North, 270 is East
        if (yaw >= 337.5 || yaw < 22.5) {
            return "South";
        } else if (yaw >= 22.5 && yaw < 67.5) {
            return "Southwest";
        } else if (yaw >= 67.5 && yaw < 112.5) {
            return "West";
        } else if (yaw >= 112.5 && yaw < 157.5) {
            return "Northwest";
        } else if (yaw >= 157.5 && yaw < 202.5) {
            return "North";
        } else if (yaw >= 202.5 && yaw < 247.5) {
            return "Northeast";
        } else if (yaw >= 247.5 && yaw < 292.5) {
            return "East";
        } else { // 292.5 to 337.5
            return "Southeast";
        }
    }
}