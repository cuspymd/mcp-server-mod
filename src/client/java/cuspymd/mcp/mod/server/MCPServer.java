package cuspymd.mcp.mod.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import cuspymd.mcp.mod.config.MCPConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class MCPServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(MCPServer.class);
    private static final Gson GSON = new Gson();
    
    private final MCPRequestHandler requestHandler;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread serverThread;
    
    public MCPServer(MCPConfig config) {
        this.requestHandler = new MCPRequestHandler(config);
    }
    
    public void start() throws IOException {
        if (running.get()) {
            return;
        }
        
        running.set(true);
        serverThread = new Thread(this::runStdioLoop);
        serverThread.setDaemon(true);
        serverThread.setName("MCP-Stdio-Server");
        serverThread.start();
        
        LOGGER.info("MCP Server started with stdio transport");
    }
    
    private void runStdioLoop() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
             PrintWriter writer = new PrintWriter(System.out, true)) {
            
            while (running.get()) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                
                try {
                    JsonObject request = JsonParser.parseString(line).getAsJsonObject();
                    JsonObject response = handleRequest(request);
                    writer.println(GSON.toJson(response));
                } catch (Exception e) {
                    LOGGER.error("Error processing request: " + line, e);
                    JsonObject errorResponse = createErrorResponse("Error processing request: " + e.getMessage());
                    writer.println(GSON.toJson(errorResponse));
                }
            }
        } catch (IOException e) {
            LOGGER.error("Error in stdio loop", e);
        }
    }
    
    private JsonObject handleRequest(JsonObject request) {
        String method = request.get("method").getAsString();
        JsonObject params = request.has("params") ? request.getAsJsonObject("params") : new JsonObject();
        
        switch (method) {
            case "initialize":
                return requestHandler.handleInitialize(params);
            case "ping":
                return requestHandler.handlePing(params);
            case "tools/list":
                return requestHandler.handleToolsList(params);
            case "tools/call":
                return requestHandler.handleToolsCall(params);
            default:
                return createErrorResponse("Unknown method: " + method);
        }
    }
    
    private JsonObject createErrorResponse(String message) {
        JsonObject response = new JsonObject();
        response.addProperty("isError", true);
        response.addProperty("error", message);
        return response;
    }
    
    public void stop() {
        if (running.get()) {
            running.set(false);
            if (serverThread != null) {
                serverThread.interrupt();
            }
            LOGGER.info("MCP Server stopped");
        }
    }
}