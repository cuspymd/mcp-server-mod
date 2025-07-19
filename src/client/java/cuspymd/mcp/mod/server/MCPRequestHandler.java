package cuspymd.mcp.mod.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import cuspymd.mcp.mod.command.CommandExecutor;
import cuspymd.mcp.mod.config.MCPConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class MCPRequestHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(MCPRequestHandler.class);
    private static final Gson GSON = new Gson();
    
    private final MCPConfig config;
    private final CommandExecutor commandExecutor;
    
    public MCPRequestHandler(MCPConfig config) {
        this.config = config;
        this.commandExecutor = new CommandExecutor(config);
    }
    
    public void handleInitialize(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }
        
        JsonObject response = new JsonObject();
        response.addProperty("protocolVersion", "2024-11-05");
        
        JsonObject capabilities = new JsonObject();
        capabilities.addProperty("tools", true);
        response.add("capabilities", capabilities);
        
        JsonObject serverInfo = new JsonObject();
        serverInfo.addProperty("name", "minecraft-mcp-client");
        serverInfo.addProperty("version", "1.0.0");
        response.add("serverInfo", serverInfo);
        
        sendResponse(exchange, 200, response);
    }
    
    public void handlePing(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }
        
        JsonObject response = new JsonObject();
        response.addProperty("status", "pong");
        sendResponse(exchange, 200, response);
    }
    
    public void handleToolsList(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }
        
        JsonObject response = new JsonObject();
        response.add("tools", MCPProtocol.getToolsListResponse());
        sendResponse(exchange, 200, response);
    }
    
    public void handleToolsCall(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }
        
        try {
            String requestBody = readRequestBody(exchange);
            JsonObject request = JsonParser.parseString(requestBody).getAsJsonObject();
            
            JsonObject params = request.getAsJsonObject("params");
            String toolName = params.get("name").getAsString();
            JsonObject arguments = params.getAsJsonObject("arguments");
            
            if ("execute_commands".equals(toolName)) {
                JsonObject result = commandExecutor.executeCommands(arguments);
                sendResponse(exchange, 200, result);
            } else {
                sendError(exchange, 400, "Unknown tool: " + toolName);
            }
        } catch (Exception e) {
            LOGGER.error("Error handling tools/call request", e);
            sendError(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }
    
    private String readRequestBody(HttpExchange exchange) throws IOException {
        InputStream inputStream = exchange.getRequestBody();
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }
    
    private void sendResponse(HttpExchange exchange, int statusCode, JsonObject response) throws IOException {
        String jsonResponse = GSON.toJson(response);
        byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Accept", "application/json");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
    
    private void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        JsonObject error = new JsonObject();
        error.addProperty("isError", true);
        error.addProperty("error", message);
        sendResponse(exchange, statusCode, error);
    }
}