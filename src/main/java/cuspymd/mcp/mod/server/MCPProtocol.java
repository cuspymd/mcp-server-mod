package cuspymd.mcp.mod.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import cuspymd.mcp.mod.config.MCPConfig;

public class MCPProtocol {
    
    public static JsonArray getToolsListResponse(MCPConfig config) {
        JsonArray tools = new JsonArray();
        
        // Execute commands tool
        JsonObject executeCommandsTool = new JsonObject();
        executeCommandsTool.addProperty("name", "execute_commands");
        executeCommandsTool.addProperty("description", "Execute one or more Minecraft commands sequentially");
        
        JsonObject inputSchema = new JsonObject();
        inputSchema.addProperty("type", "object");
        
        JsonObject properties = new JsonObject();
        
        JsonObject commandsProperty = new JsonObject();
        commandsProperty.addProperty("type", "array");
        commandsProperty.addProperty("description", "Array of Minecraft commands to execute (without leading slash)");
        commandsProperty.addProperty("minItems", 1);
        
        JsonObject commandsItems = new JsonObject();
        commandsItems.addProperty("type", "string");
        commandsProperty.add("items", commandsItems);
        
        JsonObject validateSafetyProperty = new JsonObject();
        validateSafetyProperty.addProperty("type", "boolean");
        validateSafetyProperty.addProperty("description", "Whether to validate command safety (default: true)");
        validateSafetyProperty.addProperty("default", true);
        
        properties.add("commands", commandsProperty);
        properties.add("validate_safety", validateSafetyProperty);
        inputSchema.add("properties", properties);
        
        JsonArray required = new JsonArray();
        required.add("commands");
        inputSchema.add("required", required);
        
        executeCommandsTool.add("inputSchema", inputSchema);
        tools.add(executeCommandsTool);
        
        // Get player info tool
        JsonObject getPlayerInfoTool = new JsonObject();
        getPlayerInfoTool.addProperty("name", "get_player_info");
        getPlayerInfoTool.addProperty("description", "Get comprehensive player information including: exact position (x,y,z) and block coordinates, facing direction (yaw/pitch/cardinal direction), calculated front position for building (3 blocks ahead), look vector, health and food status, game mode, dimension and time info, experience level, and inventory details (selected slot, main/off-hand items). Essential for accurate building placement and contextual command execution.");
        
        JsonObject playerInfoInputSchema = new JsonObject();
        playerInfoInputSchema.addProperty("type", "object");
        
        JsonObject playerInfoProperties = new JsonObject();
        // No required parameters for this tool
        playerInfoInputSchema.add("properties", playerInfoProperties);
        
        getPlayerInfoTool.add("inputSchema", playerInfoInputSchema);
        tools.add(getPlayerInfoTool);
        
        // Get blocks in area tool
        JsonObject getBlocksInAreaTool = new JsonObject();
        getBlocksInAreaTool.addProperty("name", "get_blocks_in_area");
        int maxAreaSize = config != null ? config.getServer().getMaxAreaSize() : 10;
        getBlocksInAreaTool.addProperty("description", "Get all non-air blocks in a specified area. Maximum area size per axis is limited (current: " + maxAreaSize + " blocks). Air blocks are excluded from results.");
        
        JsonObject blocksInputSchema = new JsonObject();
        blocksInputSchema.addProperty("type", "object");
        
        JsonObject blocksProperties = new JsonObject();
        
        // From position
        JsonObject fromProperty = new JsonObject();
        fromProperty.addProperty("type", "object");
        fromProperty.addProperty("description", "Starting position of the area to scan");
        JsonObject fromPosProperties = new JsonObject();
        JsonObject xProp = new JsonObject();
        xProp.addProperty("type", "integer");
        JsonObject yProp = new JsonObject();
        yProp.addProperty("type", "integer");
        JsonObject zProp = new JsonObject();
        zProp.addProperty("type", "integer");
        fromPosProperties.add("x", xProp);
        fromPosProperties.add("y", yProp);
        fromPosProperties.add("z", zProp);
        fromProperty.add("properties", fromPosProperties);
        JsonArray fromRequired = new JsonArray();
        fromRequired.add("x");
        fromRequired.add("y");
        fromRequired.add("z");
        fromProperty.add("required", fromRequired);
        
        // To position
        JsonObject toProperty = new JsonObject();
        toProperty.addProperty("type", "object");
        toProperty.addProperty("description", "Ending position of the area to scan");
        JsonObject toPosProperties = new JsonObject();
        toPosProperties.add("x", xProp);
        toPosProperties.add("y", yProp);
        toPosProperties.add("z", zProp);
        toProperty.add("properties", toPosProperties);
        JsonArray toRequired = new JsonArray();
        toRequired.add("x");
        toRequired.add("y");
        toRequired.add("z");
        toProperty.add("required", toRequired);
        
        blocksProperties.add("from", fromProperty);
        blocksProperties.add("to", toProperty);
        blocksInputSchema.add("properties", blocksProperties);
        
        JsonArray blocksRequiredFields = new JsonArray();
        blocksRequiredFields.add("from");
        blocksRequiredFields.add("to");
        blocksInputSchema.add("required", blocksRequiredFields);
        
        getBlocksInAreaTool.add("inputSchema", blocksInputSchema);
        tools.add(getBlocksInAreaTool);
        
        // Take screenshot tool
        JsonObject takeScreenshotTool = new JsonObject();
        takeScreenshotTool.addProperty("name", "take_screenshot");
        takeScreenshotTool.addProperty("description", "Capture a screenshot of the current Minecraft game screen. " +
                "This allows you to visually inspect the world, your builds, or the player's surroundings. " +
                "Optionally, you can specify coordinates and rotation to move the player and set their gaze before taking the screenshot. " +
                "IMPORTANT: If x, y, and z are provided, the player WILL be teleported to that location. " +
                "If yaw and pitch are provided, the player's camera direction WILL be changed. " +
                "Use this to get the perfect angle for inspecting structures.");

        JsonObject screenshotInputSchema = new JsonObject();
        screenshotInputSchema.addProperty("type", "object");

        JsonObject screenshotProperties = new JsonObject();

        JsonObject xCoord = new JsonObject();
        xCoord.addProperty("type", "number");
        xCoord.addProperty("description", "Optional X coordinate to teleport the player to");

        JsonObject yCoord = new JsonObject();
        yCoord.addProperty("type", "number");
        yCoord.addProperty("description", "Optional Y coordinate to teleport the player to");

        JsonObject zCoord = new JsonObject();
        zCoord.addProperty("type", "number");
        zCoord.addProperty("description", "Optional Z coordinate to teleport the player to");

        JsonObject yawProp = new JsonObject();
        yawProp.addProperty("type", "number");
        yawProp.addProperty("description", "Optional Yaw rotation (0 to 360, or -180 to 180) to set the player's horizontal view direction");

        JsonObject pitchProp = new JsonObject();
        pitchProp.addProperty("type", "number");
        pitchProp.addProperty("description", "Optional Pitch rotation (-90 to 90) to set the player's vertical view direction (looking down to up)");

        screenshotProperties.add("x", xCoord);
        screenshotProperties.add("y", yCoord);
        screenshotProperties.add("z", zCoord);
        screenshotProperties.add("yaw", yawProp);
        screenshotProperties.add("pitch", pitchProp);

        screenshotInputSchema.add("properties", screenshotProperties);
        takeScreenshotTool.add("inputSchema", screenshotInputSchema);
        tools.add(takeScreenshotTool);

        return tools;
    }
    
    public static JsonObject createSuccessResponse(String message) {
        JsonObject response = new JsonObject();
        response.addProperty("isError", false);
        
        JsonArray content = new JsonArray();
        JsonObject textContent = new JsonObject();
        textContent.addProperty("type", "text");
        textContent.addProperty("text", message);
        content.add(textContent);
        
        response.add("content", content);
        return response;
    }
    
    public static JsonObject createErrorResponse(String message, JsonObject meta) {
        JsonObject response = new JsonObject();
        response.addProperty("isError", true);
        
        JsonArray content = new JsonArray();
        JsonObject textContent = new JsonObject();
        textContent.addProperty("type", "text");
        textContent.addProperty("text", message);
        content.add(textContent);
        
        response.add("content", content);
        if (meta != null) {
            response.add("_meta", meta);
        }
        
        return response;
    }

    public static JsonObject createImageResponse(String base64Data, String mimeType) {
        JsonObject response = new JsonObject();
        response.addProperty("isError", false);

        JsonArray content = new JsonArray();
        JsonObject imageContent = new JsonObject();
        imageContent.addProperty("type", "image");
        imageContent.addProperty("data", base64Data);
        imageContent.addProperty("mimeType", mimeType);
        content.add(imageContent);

        response.add("content", content);
        return response;
    }
}