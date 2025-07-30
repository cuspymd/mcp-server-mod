package cuspymd.mcp.mod.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class MCPProtocol {
    
    public static JsonArray getToolsListResponse() {
        JsonArray tools = new JsonArray();
        
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