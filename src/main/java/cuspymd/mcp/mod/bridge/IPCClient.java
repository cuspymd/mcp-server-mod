package cuspymd.mcp.mod.bridge;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;

public class IPCClient {
    private static final Gson GSON = new Gson();
    private static final int IPC_PORT = 25565;
    private static final String IPC_HOST = "localhost";
    
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    
    public boolean connect() {
        try {
            socket = new Socket(IPC_HOST, IPC_PORT);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    public JsonObject executeCommands(JsonObject arguments) {
        if (socket == null || socket.isClosed()) {
            return createErrorResponse("Not connected to IPC server");
        }
        
        try {
            JsonObject request = new JsonObject();
            request.addProperty("type", "execute_commands");
            request.add("arguments", arguments);
            
            writer.println(GSON.toJson(request));
            
            String response = reader.readLine();
            if (response != null) {
                return JsonParser.parseString(response).getAsJsonObject();
            } else {
                return createErrorResponse("No response from IPC server");
            }
        } catch (IOException e) {
            return createErrorResponse("IPC communication error: " + e.getMessage());
        }
    }
    
    public CompletableFuture<Boolean> connectAsync() {
        return CompletableFuture.supplyAsync(() -> {
            int attempts = 0;
            while (attempts < 10) {
                if (connect()) {
                    return true;
                }
                attempts++;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            return false;
        });
    }
    
    private JsonObject createErrorResponse(String message) {
        JsonObject response = new JsonObject();
        response.addProperty("isError", true);
        response.addProperty("error", message);
        return response;
    }
    
    public void disconnect() {
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            // Error handled silently
        }
    }
    
    public boolean isConnected() {
        return socket != null && !socket.isClosed();
    }
}