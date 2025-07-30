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