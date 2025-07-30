package cuspymd.mcp.mod.bridge;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import cuspymd.mcp.mod.command.CommandExecutor;
import cuspymd.mcp.mod.config.MCPConfig;
import cuspymd.mcp.mod.server.MCPProtocol;
import cuspymd.mcp.mod.utils.PlayerInfoProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class HTTPMCPServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(HTTPMCPServer.class);
    private static final Gson GSON = new Gson();
    
    private final MCPConfig config;
    private final CommandExecutor commandExecutor;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private HttpServer httpServer;
    private ExecutorService executor;
    
    public HTTPMCPServer(MCPConfig config) {
        this.config = config;
        this.commandExecutor = new CommandExecutor(config);
    }
    
    public void start() throws IOException {
        if (running.get()) {
            return;
        }
        
        running.set(true);
        
        InetSocketAddress address = new InetSocketAddress(
            config.getServer().getHost(), 
            config.getServer().getPort()
        );
        
        httpServer = HttpServer.create(address, 0);
        httpServer.createContext("/mcp", new MCPHandler());
        
        executor = Executors.newCachedThreadPool();
        httpServer.setExecutor(executor);
        
        httpServer.start();
        
        LOGGER.info("HTTP MCP Server started on http://{}:{}/mcp", 
            config.getServer().getHost(), 
            config.getServer().getPort());
    }
    
    public void stop() {
        if (running.get()) {
            running.set(false);
            
            if (httpServer != null) {
                httpServer.stop(0);
            }
            
            if (executor != null) {
                executor.shutdown();
            }
            
            LOGGER.info("HTTP MCP Server stopped");
        }
    }
    
    public int getPort() {
        return config.getServer().getPort();
    }
    
    private class MCPHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                // Log request method and body
                String method = exchange.getRequestMethod();
                String accept = exchange.getRequestHeaders().getFirst("Accept");
                String requestBody = "";
                if ("POST".equals(method)) {
                    requestBody = readRequestBody(exchange);
                }
                LOGGER.info("MCPHandler received request - Method: {}, Accept: {}, Body: {}", method, accept, requestBody);
                
                // Validate Origin header for security (basic check)
                String origin = exchange.getRequestHeaders().getFirst("Origin");
                if (origin != null && !isAllowedOrigin(origin)) {
                    sendErrorResponse(exchange, 403, "Forbidden origin", null);
                    return;
                }
                
                if ("POST".equals(method)) {
                    handlePostRequest(exchange, requestBody);
                } else if ("GET".equals(method)) {
                    handleGetRequest(exchange);
                } else {
                    sendErrorResponse(exchange, 405, "Method not allowed", null);
                }
            } catch (Exception e) {
                LOGGER.error("Error handling MCP request", e);
                sendErrorResponse(exchange, 500, "Internal server error", null);
            }
        }
        
        private void handlePostRequest(HttpExchange exchange, String requestBody) throws IOException {
            // Check Accept headers
            String accept = exchange.getRequestHeaders().getFirst("Accept");
            if (accept == null || (!accept.contains("application/json") && !accept.contains("text/event-stream"))) {
                sendErrorResponse(exchange, 400, "Invalid Accept header", null);
                return;
            }
            
            try {
                JsonObject request = JsonParser.parseString(requestBody).getAsJsonObject();
                LOGGER.info("Received HTTP MCP request: {}", requestBody);
                
                JsonObject response = handleMCPRequest(request);
                
                if (response != null) {
                    LOGGER.info("Sending HTTP MCP response: {}", response);
                    sendJsonResponse(exchange, 200, response);
                } else {
                    // Notification - no response needed
                    sendJsonResponse(exchange, 202, new JsonObject());
                }
                
            } catch (Exception e) {
                LOGGER.error("Error processing MCP request: " + requestBody, e);
                Object requestId = null;
                try {
                    JsonObject request = JsonParser.parseString(requestBody).getAsJsonObject();
                    requestId = request.has("id") ? request.get("id") : null;
                } catch (Exception ignored) {
                    // Unable to parse request ID
                }
                JsonObject errorResponse = createErrorResponse("Error processing request: " + e.getMessage(), requestId);
                sendJsonResponse(exchange, 400, errorResponse);
            }
        }
        
        private void handleGetRequest(HttpExchange exchange) throws IOException {
            // Check for SSE support
            String accept = exchange.getRequestHeaders().getFirst("Accept");
            if (accept != null && accept.contains("text/event-stream")) {
                // For now, we'll just send a simple response indicating SSE is not fully implemented
                // In a full implementation, this would open an SSE stream
                sendErrorResponse(exchange, 501, "Server-Sent Events not implemented", null);
            } else {
                sendErrorResponse(exchange, 400, "GET requests require text/event-stream Accept header", null);
            }
        }
        
        private String readRequestBody(HttpExchange exchange) throws IOException {
            try (InputStream inputStream = exchange.getRequestBody();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                
                StringBuilder body = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    body.append(line);
                }
                return body.toString();
            }
        }
        
        private void sendJsonResponse(HttpExchange exchange, int statusCode, JsonObject response) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Accept, Origin");
            
            byte[] responseBytes = GSON.toJson(response).getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(responseBytes);
            }
        }
        
        private void sendErrorResponse(HttpExchange exchange, int statusCode, String message, Object requestId) throws IOException {
            JsonObject errorResponse = createErrorResponse(message, requestId);
            sendJsonResponse(exchange, statusCode, errorResponse);
        }
        
        private boolean isAllowedOrigin(String origin) {
            // Basic localhost and file protocol check
            return origin.startsWith("http://localhost") || 
                   origin.startsWith("https://localhost") ||
                   origin.startsWith("http://127.0.0.1") ||
                   origin.startsWith("https://127.0.0.1") ||
                   origin.equals("null") || // file:// protocol
                   origin.startsWith("file://");
        }
    }
    
    private JsonObject handleMCPRequest(JsonObject request) {
        String method = request.get("method").getAsString();
        JsonObject params = request.has("params") ? request.getAsJsonObject("params") : new JsonObject();
        Object requestId = request.has("id") ? request.get("id") : null;
        
        // Handle notifications - no response needed
        if (method.startsWith("notifications/")) {
            LOGGER.info("Received notification: {}", method);
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
        
        // Check client's protocol version and respond accordingly
        String currentProtocolVersion = "2025-06-18"; // default
        if (params.has("protocolVersion")) {
            String clientVersion = params.get("protocolVersion").getAsString();
            if ("2025-03-26".equals(clientVersion)) {
                currentProtocolVersion = "2025-03-26";
            }
        }
        response.addProperty("protocolVersion", currentProtocolVersion);
        
        JsonObject capabilities = new JsonObject();
        capabilities.add("tools", new JsonObject());
        response.add("capabilities", capabilities);
        
        JsonObject serverInfo = new JsonObject();
        serverInfo.addProperty("name", "minecraft-mcp-http");
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
        response.add("tools", MCPProtocol.getToolsListResponse());
        return response;
    }
    
    private JsonObject handleToolsCall(JsonObject params) {
        try {
            String toolName = params.get("name").getAsString();
            JsonObject arguments = params.getAsJsonObject("arguments");
            
            if ("execute_commands".equals(toolName)) {
                return commandExecutor.executeCommands(arguments);
            } else if ("get_player_info".equals(toolName)) {
                return handleGetPlayerInfo(arguments);
            } else {
                JsonObject error = new JsonObject();
                error.addProperty("isError", true);
                error.addProperty("error", "Unknown tool: " + toolName);
                return error;
            }
        } catch (Exception e) {
            LOGGER.error("Error handling tools/call request: {}", e.getMessage());
            JsonObject error = new JsonObject();
            error.addProperty("isError", true);
            error.addProperty("error", "Internal server error: " + e.getMessage());
            return error;
        }
    }
    
    private JsonObject handleGetPlayerInfo(JsonObject arguments) {
        try {
            JsonObject playerInfo = PlayerInfoProvider.getPlayerInfo();
            
            // Check if there was an error getting player info
            if (playerInfo.has("error")) {
                return MCPProtocol.createErrorResponse(playerInfo.get("error").getAsString(), null);
            }
            
            // Create success response with player information
            return MCPProtocol.createSuccessResponse(playerInfo.toString());
            
        } catch (Exception e) {
            LOGGER.error("Error getting player info: {}", e.getMessage());
            return MCPProtocol.createErrorResponse("Failed to get player information: " + e.getMessage(), null);
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
}