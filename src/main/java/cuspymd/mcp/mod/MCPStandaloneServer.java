package cuspymd.mcp.mod;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import cuspymd.mcp.mod.bridge.IPCClient;
import cuspymd.mcp.mod.server.MCPProtocol;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;

public class MCPStandaloneServer {
    private static final Gson GSON = new Gson();
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final String LOG_FILE_PATH = System.getProperty("user.home") + File.separator + "standalone.log";
    
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final IPCClient ipcClient;
    
    public MCPStandaloneServer() {
        this.ipcClient = new IPCClient();
        initializeLogFile();
    }
    
    private void initializeLogFile() {
        try {
            Path logPath = Paths.get(LOG_FILE_PATH);
            Files.createDirectories(logPath.getParent());
            if (!Files.exists(logPath)) {
                Files.createFile(logPath);
            }
            logToFile("=== MCP Standalone Server Started ===");
        } catch (IOException e) {
            // Error handled silently
        }
    }
    
    private void logToFile(String message) {
        try {
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            String logEntry = timestamp + " " + message + System.lineSeparator();
            Files.write(Paths.get(LOG_FILE_PATH), logEntry.getBytes(), 
                       StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        } catch (IOException e) {
            // Error handled silently
        }
    }
    
    public static void main(String[] args) {
        try {
            MCPStandaloneServer server = new MCPStandaloneServer();
            server.run();
        } catch (Exception e) {
            System.exit(1);
        }
    }
    
    public void run() {
        logToFile("Starting MCP Standalone Server with stdio transport");
        
        // Try to connect to IPC server
        ipcClient.connectAsync().thenAccept(connected -> {
            if (connected) {
                logToFile("Connected to Minecraft IPC server");
            } else {
                logToFile("Failed to connect to Minecraft IPC server - commands will not work");
            }
        });
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
             PrintWriter writer = new PrintWriter(System.out, true)) {
            
            while (running.get()) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                
                logToFile("Received: " + line);
                
                try {
                    JsonObject request = JsonParser.parseString(line).getAsJsonObject();
                    JsonObject response = handleRequest(request);
                    if (response != null) {
                        String responseStr = GSON.toJson(response);
                        logToFile("Sending: " + responseStr);
                        writer.println(responseStr);
                    }
                } catch (Exception e) {
                    logToFile("Error processing request: " + line + " - " + e.getMessage());
                    Object requestId = null;
                    try {
                        JsonObject request = JsonParser.parseString(line).getAsJsonObject();
                        requestId = request.has("id") ? request.get("id") : null;
                    } catch (Exception ignored) {
                        // Unable to parse request ID
                    }
                    JsonObject errorResponse = createErrorResponse("Error processing request: " + e.getMessage(), requestId);
                    writer.println(GSON.toJson(errorResponse));
                }
            }
        } catch (IOException e) {
            logToFile("Error in stdio loop: " + e.getMessage());
        } finally {
            ipcClient.disconnect();
            logToFile("=== MCP Standalone Server Stopped ===");
        }
    }
    
    private JsonObject handleRequest(JsonObject request) {
        String method = request.get("method").getAsString();
        JsonObject params = request.has("params") ? request.getAsJsonObject("params") : new JsonObject();
        Object requestId = request.has("id") ? request.get("id") : null;
        
        // Handle notifications - no response needed
        if (method.startsWith("notifications/")) {
            logToFile("Received notification: " + method);
            return null;
        }
        
        JsonObject result;
        switch (method) {
            case "initialize":
                result = handleInitialize(params);
                break;
            case "ping":
                result = handlePing(params);
                break;
            case "tools/list":
                result = handleToolsList(params);
                break;
            case "tools/call":
                result = handleToolsCall(params);
                break;
            default:
                return createErrorResponse("Unknown method: " + method, requestId);
        }
        
        return createSuccessResponse(result, requestId);
    }
    
    private JsonObject handleInitialize(JsonObject params) {
        JsonObject response = new JsonObject();
        response.addProperty("protocolVersion", "2025-06-18");
        
        JsonObject capabilities = new JsonObject();
        capabilities.add("tools", new JsonObject());
        response.add("capabilities", capabilities);
        
        JsonObject serverInfo = new JsonObject();
        serverInfo.addProperty("name", "minecraft-mcp-standalone");
        serverInfo.addProperty("version", "1.0.0");
        response.add("serverInfo", serverInfo);
        
        return response;
    }
    
    private JsonObject handlePing(JsonObject params) {
        JsonObject response = new JsonObject();
        response.addProperty("status", "pong");
        return response;
    }
    
    private JsonObject handleToolsList(JsonObject params) {
        JsonObject response = new JsonObject();
        response.add("tools", GSON.toJsonTree(MCPProtocol.getToolsList()));
        return response;
    }
    
    private JsonObject handleToolsCall(JsonObject params) {
        try {
            String toolName = params.get("name").getAsString();
            JsonObject arguments = params.getAsJsonObject("arguments");
            
            if ("execute_commands".equals(toolName)) {
                if (!ipcClient.isConnected()) {
                    logToFile("IPC client not connected, attempting to reconnect...");
                    boolean connected = ipcClient.connectAsync().get();
                    if (!connected) {
                        JsonObject error = new JsonObject();
                        error.addProperty("isError", true);
                        error.addProperty("error", "Failed to connect to Minecraft client");
                        return error;
                    }
                    logToFile("Successfully reconnected to Minecraft IPC server");
                }
                return ipcClient.executeCommands(arguments);
            } else {
                JsonObject error = new JsonObject();
                error.addProperty("isError", true);
                error.addProperty("error", "Unknown tool: " + toolName);
                return error;
            }
        } catch (Exception e) {
            logToFile("Error handling tools/call request: " + e.getMessage());
            JsonObject error = new JsonObject();
            error.addProperty("isError", true);
            error.addProperty("error", "Internal server error: " + e.getMessage());
            return error;
        }
    }
    
    private JsonObject createSuccessResponse(JsonObject result, Object requestId) {
        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", "2.0");
        if (requestId != null) {
            if (requestId instanceof Number) {
                response.addProperty("id", ((Number) requestId).intValue());
            } else {
                response.addProperty("id", requestId.toString());
            }
        }
        response.add("result", result);
        return response;
    }
    
    private JsonObject createErrorResponse(String message, Object requestId) {
        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", "2.0");
        if (requestId != null) {
            if (requestId instanceof Number) {
                response.addProperty("id", ((Number) requestId).intValue());
            } else {
                response.addProperty("id", requestId.toString());
            }
        }
        
        JsonObject error = new JsonObject();
        error.addProperty("code", -32603); // Internal error
        error.addProperty("message", message);
        response.add("error", error);
        
        return response;
    }
    
    public void stop() {
        running.set(false);
        ipcClient.disconnect();
    }
}