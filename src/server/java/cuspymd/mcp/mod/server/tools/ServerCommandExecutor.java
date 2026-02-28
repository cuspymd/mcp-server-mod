package cuspymd.mcp.mod.server.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import cuspymd.mcp.mod.command.ICommandExecutor;
import cuspymd.mcp.mod.config.MCPConfig;
import cuspymd.mcp.mod.server.MCPProtocol;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ServerCommandExecutor implements ICommandExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerCommandExecutor.class);
    private final MCPConfig config;
    private final MinecraftServer server;

    public ServerCommandExecutor(MCPConfig config, MinecraftServer server) {
        this.config = config;
        this.server = server;
    }

    @Override
    public JsonObject executeCommands(JsonObject arguments) {
        if (!arguments.has("commands")) {
            return MCPProtocol.createErrorResponse("Missing required parameter: commands", null);
        }

        JsonArray commandsArray = arguments.getAsJsonArray("commands");
        int totalCommands = commandsArray.size();

        JsonArray results = new JsonArray();
        int acceptedCount = 0;
        int failedCount = 0;

        List<String> allMessages = new ArrayList<>();

        for (int i = 0; i < totalCommands; i++) {
            JsonElement elem = commandsArray.get(i);
            if (!elem.isJsonPrimitive() || !elem.getAsJsonPrimitive().isString()) {
                continue;
            }
            final String originalCommand = elem.getAsString();
            final String command = originalCommand.startsWith("/") ? originalCommand.substring(1) : originalCommand;

            JsonObject resultObj = new JsonObject();
            resultObj.addProperty("index", i);
            resultObj.addProperty("command", originalCommand);

            try {
                // Execute on main server thread
                JsonObject executionData = server.submit(() -> {
                    List<String> messages = new ArrayList<>();

                    // Create a capturing command source based on the server source
                    ServerCommandSource originalSource = server.getCommandSource();
                    ServerCommandSource capturingSource = originalSource.withOutput(new net.minecraft.server.command.CommandOutput() {
                        @Override
                        public void sendMessage(Text message) {
                            messages.add(message.getString());
                        }

                        @Override
                        public boolean shouldReceiveFeedback() {
                            return true;
                        }

                        @Override
                        public boolean shouldTrackOutput() {
                            return true;
                        }

                        @Override
                        public boolean shouldBroadcastConsoleToOps() {
                            return false;
                        }
                    });

                    int successCount = server.getCommandManager().executeWithPrefix(capturingSource, command);

                    JsonObject partial = new JsonObject();
                    partial.addProperty("successCount", successCount);
                    JsonArray messagesArray = new JsonArray();
                    for(String m : messages) messagesArray.add(m);
                    partial.add("messages", messagesArray);

                    return partial;
                }).get();

                int successCount = executionData.get("successCount").getAsInt();
                JsonArray msgs = executionData.getAsJsonArray("messages");

                JsonArray perCommandMessages = new JsonArray();
                for (JsonElement m : msgs) {
                    perCommandMessages.add(m.getAsString());
                    allMessages.add(m.getAsString());
                }

                resultObj.add("chatMessages", perCommandMessages);

                if (successCount > 0) {
                    acceptedCount++;
                    resultObj.addProperty("status", "success");
                    resultObj.addProperty("accepted", true);
                    resultObj.addProperty("applied", true);
                    resultObj.addProperty("summary", "Command executed successfully. Feedback: " + (msgs.size() > 0 ? msgs.get(0).getAsString() : ""));
                } else {
                    failedCount++;
                    resultObj.addProperty("status", "failed");
                    resultObj.addProperty("accepted", true);
                    resultObj.addProperty("applied", false);
                    resultObj.addProperty("summary", "Command failed to execute. Feedback: " + (msgs.size() > 0 ? msgs.get(0).getAsString() : ""));
                }
            } catch (Exception e) {
                LOGGER.error("Error executing server command: " + command, e);
                failedCount++;
                resultObj.addProperty("status", "error");
                resultObj.addProperty("accepted", false);
                resultObj.addProperty("applied", false);
                resultObj.addProperty("summary", "Error: " + e.getMessage());
            }

            results.add(resultObj);
        }

        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("totalCommands", totalCommands);
        responseJson.addProperty("acceptedCount", acceptedCount);
        responseJson.addProperty("appliedCount", acceptedCount);
        responseJson.addProperty("failedCount", failedCount);
        responseJson.add("results", results);
        JsonArray allMessagesArray = new JsonArray();
        for (String msg : allMessages) {
            allMessagesArray.add(msg);
        }
        responseJson.add("chatMessages", allMessagesArray);
        responseJson.addProperty("hint", "Use get_blocks_in_area to verify the built structure and fix any issues.");

        return responseJson;
    }
}