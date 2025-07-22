package cuspymd.mcp.mod.bridge;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import cuspymd.mcp.mod.command.CommandExecutor;
import cuspymd.mcp.mod.config.MCPConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class IPCServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(IPCServer.class);
    private static final Gson GSON = new Gson();
    private static final int IPC_PORT = 25565; // Default port for IPC
    
    private final MCPConfig config;
    private final CommandExecutor commandExecutor;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ServerSocket serverSocket;
    private ExecutorService executor;
    
    public IPCServer(MCPConfig config) {
        this.config = config;
        this.commandExecutor = new CommandExecutor(config);
    }
    
    public void start() throws IOException {
        if (running.get()) {
            return;
        }
        
        running.set(true);
        serverSocket = new ServerSocket(IPC_PORT);
        executor = Executors.newCachedThreadPool();
        
        Thread acceptThread = new Thread(this::acceptConnections);
        acceptThread.setDaemon(true);
        acceptThread.setName("IPC-Accept-Thread");
        acceptThread.start();
        
        LOGGER.info("IPC Server started on port {}", IPC_PORT);
    }
    
    private void acceptConnections() {
        while (running.get() && !serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();
                executor.submit(() -> handleClient(clientSocket));
            } catch (IOException e) {
                if (running.get()) {
                    LOGGER.error("Error accepting IPC connection", e);
                }
            }
        }
    }
    
    private void handleClient(Socket clientSocket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)) {
            
            LOGGER.info("IPC client connected from {}", clientSocket.getRemoteSocketAddress());
            
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    LOGGER.info("Received IPC request: {}", line);
                    JsonObject request = JsonParser.parseString(line).getAsJsonObject();
                    JsonObject response = handleIPCRequest(request);
                    LOGGER.info("Sending IPC response: {}", response);

                    writer.println(GSON.toJson(response));
                } catch (Exception e) {
                    LOGGER.error("Error processing IPC request: " + line, e);
                    JsonObject errorResponse = createErrorResponse("Error processing request: " + e.getMessage());
                    writer.println(GSON.toJson(errorResponse));
                }
            }
        } catch (IOException e) {
            LOGGER.error("Error handling IPC client", e);
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                LOGGER.error("Error closing IPC client socket", e);
            }
        }
    }
    
    private JsonObject handleIPCRequest(JsonObject request) {
        String type = request.get("type").getAsString();
        
        if ("execute_commands".equals(type)) {
            JsonObject arguments = request.getAsJsonObject("arguments");
            return commandExecutor.executeCommands(arguments);
        } else {
            return createErrorResponse("Unknown IPC request type: " + type);
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
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
                if (executor != null) {
                    executor.shutdown();
                }
                LOGGER.info("IPC Server stopped");
            } catch (IOException e) {
                LOGGER.error("Error stopping IPC server", e);
            }
        }
    }
    
    public int getPort() {
        return IPC_PORT;
    }
}