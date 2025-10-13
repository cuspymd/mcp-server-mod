package cuspymd.mcp.mod.utils;

import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class PlayerInfoProvider {
    
    public static JsonObject getPlayerInfo() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        World world = client.world;
        
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
        rotation.addProperty("yaw", player.getYaw());
        rotation.addProperty("pitch", player.getPitch());
        playerInfo.add("rotation", rotation);
        
        // Cardinal direction (N, S, E, W, NE, NW, SE, SW)
        String direction = getCardinalDirection(player.getYaw());
        playerInfo.addProperty("facingDirection", direction);
        
        // Look vector (normalized direction the player is looking)
        Vec3d lookVec = player.getRotationVec(1.0f);
        JsonObject lookVector = new JsonObject();
        lookVector.addProperty("x", lookVec.x);
        lookVector.addProperty("y", lookVec.y);
        lookVector.addProperty("z", lookVec.z);
        playerInfo.add("lookVector", lookVector);
        
        // Calculate position in front of player (useful for building)
        Vec3d playerPos = new Vec3d(player.getX(), player.getY(), player.getZ());
        Vec3d frontPos = playerPos.add(lookVec.multiply(3.0)); // 3 blocks in front
        JsonObject frontPosition = new JsonObject();
        frontPosition.addProperty("x", (int) Math.floor(frontPos.x));
        frontPosition.addProperty("y", (int) Math.floor(frontPos.y));
        frontPosition.addProperty("z", (int) Math.floor(frontPos.z));
        playerInfo.add("frontPosition", frontPosition);
        
        // Health and food information
        playerInfo.addProperty("health", player.getHealth());
        playerInfo.addProperty("maxHealth", player.getMaxHealth());
        playerInfo.addProperty("foodLevel", player.getHungerManager().getFoodLevel());
        playerInfo.addProperty("saturation", player.getHungerManager().getSaturationLevel());
        
        // Game mode
        if (client.interactionManager != null) {
            playerInfo.addProperty("gameMode", client.interactionManager.getCurrentGameMode().name().toLowerCase());
        }
        
        // Dimension information
        if (world != null) {
            playerInfo.addProperty("dimension", world.getRegistryKey().getValue().toString());
            playerInfo.addProperty("timeOfDay", world.getTimeOfDay());
            playerInfo.addProperty("isDay", world.isDay());
            playerInfo.addProperty("isNight", world.isNight());
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
        inventory.addProperty("mainHandItem", player.getMainHandStack().isEmpty() ? "empty" : 
            player.getMainHandStack().getItem().toString());
        inventory.addProperty("offHandItem", player.getOffHandStack().isEmpty() ? "empty" : 
            player.getOffHandStack().getItem().toString());
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