package cuspymd.mcp.mod.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class MCPProtocol {
    
    public static JsonArray getToolsListResponse() {
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
        getBlocksInAreaTool.addProperty("description", "Get all non-air blocks in a specified area. Maximum area size per axis is limited (default: 50 blocks). Air blocks are excluded from results.");
        
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
}