package cuspymd.mcp.mod.server;

import com.google.gson.JsonObject;
import cuspymd.mcp.mod.command.CommandExecutor;
import cuspymd.mcp.mod.config.MCPConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MCPRequestHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(MCPRequestHandler.class);
    
    private final CommandExecutor commandExecutor;
    
    public MCPRequestHandler(MCPConfig config) {
        this.commandExecutor = new CommandExecutor(config);
    }
    
    public JsonObject handleInitialize(JsonObject params) {
        JsonObject response = new JsonObject();
        response.addProperty("protocolVersion", "2024-11-05");
        
        JsonObject capabilities = new JsonObject();
        capabilities.add("tools", new JsonObject());
        response.add("capabilities", capabilities);
        
        JsonObject serverInfo = new JsonObject();
        serverInfo.addProperty("name", "minecraft-mcp-client");
        serverInfo.addProperty("version", "1.0.0");
        response.add("serverInfo", serverInfo);
        
        return response;
    }
    
    public JsonObject handlePing(JsonObject params) {
        JsonObject response = new JsonObject();
        response.addProperty("status", "pong");
        return response;
    }
    
    public JsonObject handleToolsList(JsonObject params) {
        JsonObject response = new JsonObject();
        response.add("tools", MCPProtocol.getToolsListResponse());
        return response;
    }
    
    public JsonObject handleToolsCall(JsonObject params) {
        try {
            String toolName = params.get("name").getAsString();
            JsonObject arguments = params.getAsJsonObject("arguments");
            
            if ("execute_commands".equals(toolName)) {
                return commandExecutor.executeCommands(arguments);
            } else {
                return createErrorResponse("Unknown tool: " + toolName);
            }
        } catch (Exception e) {
            LOGGER.error("Error handling tools/call request", e);
            return createErrorResponse("Internal server error: " + e.getMessage());
        }
    }
    
    private JsonObject createErrorResponse(String message) {
        JsonObject response = new JsonObject();
        response.addProperty("isError", true);
        response.addProperty("error", message);
        return response;
    }
}