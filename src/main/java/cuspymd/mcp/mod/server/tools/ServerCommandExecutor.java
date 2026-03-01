package cuspymd.mcp.mod.server.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import cuspymd.mcp.mod.command.ICommandExecutor;
import cuspymd.mcp.mod.command.SafetyValidator;
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

    private final SafetyValidator safetyValidator;

    public ServerCommandExecutor(MCPConfig config, MinecraftServer server) {
        this.config = config;
        this.server = server;
        this.safetyValidator = new SafetyValidator(config);
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

        boolean validateSafety = !arguments.has("validate_safety") || arguments.get("validate_safety").getAsBoolean();

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

            if (validateSafety) {
                SafetyValidator.ValidationResult validationResult = safetyValidator.validate(command);
                if (!validationResult.isValid()) {
                    failedCount++;
                    resultObj.addProperty("status", "rejected_by_safety");
                    resultObj.addProperty("accepted", false);
                    resultObj.addProperty("applied", false);
                    resultObj.addProperty("summary", "Safety validation failed: " + validationResult.getErrorMessage());
                    resultObj.add("chatMessages", new JsonArray());
                    results.add(resultObj);

                    // Fail fast: Stop executing further commands
                    for (int j = i + 1; j < totalCommands; j++) {
                        JsonElement remainingElem = commandsArray.get(j);
                        if (!remainingElem.isJsonPrimitive() || !remainingElem.getAsJsonPrimitive().isString()) continue;
                        JsonObject skippedObj = new JsonObject();
                        skippedObj.addProperty("index", j);
                        skippedObj.addProperty("command", remainingElem.getAsString());
                        skippedObj.addProperty("status", "skipped");
                        skippedObj.addProperty("accepted", false);
                        skippedObj.addProperty("applied", false);
                        skippedObj.addProperty("summary", "Skipped due to previous command failing safety validation.");
                        skippedObj.add("chatMessages", new JsonArray());
                        results.add(skippedObj);
                    }
                    break; // stop processing
                }
            }

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

                    int successCount = 0;
                    try {
                        // The actual execute method for commands in this mappings version for parsing and execution
                        successCount = server.getCommandManager().getDispatcher().execute(command, capturingSource);
                    } catch (Exception ex) {
                        messages.add("Execution failed: " + ex.getMessage());
                    }

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

        return MCPProtocol.createSuccessResponse(responseJson.toString());
    }
}