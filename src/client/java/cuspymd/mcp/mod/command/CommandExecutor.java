package cuspymd.mcp.mod.command;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import cuspymd.mcp.mod.config.MCPConfig;
import cuspymd.mcp.mod.server.MCPProtocol;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;

public class CommandExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandExecutor.class);
    private static final long COMMAND_MESSAGE_WAIT_MS = 700L;
    private static final long COMMAND_MESSAGE_IDLE_MS = 120L;
    
    private final MCPConfig config;
    private final SafetyValidator safetyValidator;
    
    public CommandExecutor(MCPConfig config) {
        this.config = config;
        this.safetyValidator = new SafetyValidator(config);
    }
    
    public JsonObject executeCommands(JsonObject arguments) {
        try {
            JsonArray commandsArray = arguments.getAsJsonArray("commands");
            boolean validateSafety = !arguments.has("validate_safety") || 
                                   arguments.get("validate_safety").getAsBoolean();
            
            List<String> commands = new ArrayList<>();
            for (int i = 0; i < commandsArray.size(); i++) {
                commands.add(commandsArray.get(i).getAsString());
            }
            
            if (validateSafety) {
                for (int i = 0; i < commands.size(); i++) {
                    String command = commands.get(i);
                    SafetyValidator.ValidationResult validation = safetyValidator.validate(command);
                    if (!validation.isValid()) {
                        JsonObject responseJson = buildSafetyRejectedResponse(commands, i, validation.getErrorMessage());
                        return MCPProtocol.createSuccessResponse(responseJson.toString());
                    }
                }
            }
            
            return executeCommandsSequentially(commands);
            
        } catch (Exception e) {
            LOGGER.error("Error executing commands", e);
            return MCPProtocol.createErrorResponse("Internal error: " + e.getMessage(), null);
        }
    }
    
    private JsonObject executeCommandsSequentially(List<String> commands) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return MCPProtocol.createErrorResponse("Player or world is not available", null);
        }
        
        List<CommandResult> results = new ArrayList<>();
        List<String> allCapturedMessages = new ArrayList<>();
        
        ChatMessageCapture capture = ChatMessageCapture.getInstance();
        capture.startCapturing();
        
        try {
            capture.drainAvailableMessages();
            for (String command : commands) {
                CommandResult executionResult = executeCommandWithTimeout(command);
                List<String> commandMessages = collectMessagesForCommand(capture);
                allCapturedMessages.addAll(commandMessages);

                CommandResult analyzedResult = applyOutcomeAnalysis(executionResult, commandMessages);
                results.add(analyzedResult);
            }

            JsonObject responseJson = buildExecuteCommandsResponse(commands.size(), results, allCapturedMessages);
            return MCPProtocol.createSuccessResponse(responseJson.toString());
            
        } finally {
            capture.stopCapturing();
        }
    }
    
    private CompletableFuture<CommandResult> executeOneCommand(String command) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            
            MinecraftClient client = MinecraftClient.getInstance();
            ClientPlayerEntity player = client.player;
            
            if (player == null) {
                return CommandResult.builder()
                    .accepted(false)
                    .applied(false)
                    .status("execution_error")
                    .summary("Player is not available")
                    .originalCommand(command)
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .build();
            }
            
            try {
                String fullCommand = command.startsWith("/") ? command : "/" + command;
                
                if (config.getClient().isLogCommands()) {
                    LOGGER.info("Executing command: {}", fullCommand);
                }

                boolean hasNetworkHandler = client.getNetworkHandler() != null;
                client.execute(() -> {
                    if (client.getNetworkHandler() != null) {
                        client.getNetworkHandler().sendChatCommand(command);
                    }
                });

                if (!hasNetworkHandler) {
                    return CommandResult.builder()
                        .accepted(false)
                        .applied(false)
                        .status("execution_error")
                        .summary("Network handler is not available")
                        .originalCommand(command)
                        .executionTimeMs(System.currentTimeMillis() - startTime)
                        .build();
                }
                
                // Small delay to allow command to execute
                Thread.sleep(50);

                return CommandResult.builder()
                    .accepted(true)
                    .applied(null)
                    .status("unknown")
                    .summary("Command sent")
                    .originalCommand(command)
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .build();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return CommandResult.builder()
                    .accepted(false)
                    .applied(false)
                    .status("execution_error")
                    .summary("Command execution interrupted")
                    .originalCommand(command)
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .build();
                
            } catch (Exception e) {
                return CommandResult.builder()
                    .accepted(false)
                    .applied(false)
                    .status("execution_error")
                    .summary("Failed to execute command: " + e.getMessage())
                    .originalCommand(command)
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .build();
            }
        });
    }

    private CommandResult executeCommandWithTimeout(String command) {
        try {
            return executeOneCommand(command).get(config.getServer().getRequestTimeoutMs(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            return CommandResult.builder()
                .accepted(false)
                .applied(false)
                .status("timed_out")
                .summary("Command timed out")
                .originalCommand(command)
                .executionTimeMs(config.getServer().getRequestTimeoutMs())
                .build();
        } catch (Exception e) {
            return CommandResult.builder()
                .accepted(false)
                .applied(false)
                .status("execution_error")
                .summary("Command failed before completion: " + e.getMessage())
                .originalCommand(command)
                .executionTimeMs(0L)
                .build();
        }
    }

    private List<String> collectMessagesForCommand(ChatMessageCapture capture) {
        List<String> messages = new ArrayList<>();
        long start = System.currentTimeMillis();
        long lastMessageAt = start;

        while (System.currentTimeMillis() - start < COMMAND_MESSAGE_WAIT_MS) {
            try {
                String message = capture.waitForMessage(75);
                if (message != null) {
                    messages.add(message);
                    lastMessageAt = System.currentTimeMillis();
                    continue;
                }

                if (!messages.isEmpty() && System.currentTimeMillis() - lastMessageAt >= COMMAND_MESSAGE_IDLE_MS) {
                    break;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        messages.addAll(capture.drainAvailableMessages());
        return messages;
    }

    private CommandResult applyOutcomeAnalysis(CommandResult result, List<String> chatMessages) {
        if (!result.isAccepted()) {
            return CommandResult.builder()
                .accepted(false)
                .applied(result.getApplied())
                .status(result.getStatus())
                .summary(result.getSummary())
                .chatMessages(chatMessages)
                .originalCommand(result.getOriginalCommand())
                .executionTimeMs(result.getExecutionTimeMs())
                .build();
        }

        CommandOutcomeAnalyzer.Outcome outcome =
            CommandOutcomeAnalyzer.analyze(true, chatMessages, result.getSummary());

        return CommandResult.builder()
            .accepted(outcome.accepted())
            .applied(outcome.applied())
            .status(outcome.status())
            .summary(outcome.summary())
            .chatMessages(chatMessages)
            .originalCommand(result.getOriginalCommand())
            .executionTimeMs(result.getExecutionTimeMs())
            .build();
    }

    static JsonObject buildExecuteCommandsResponse(int totalCommands, List<CommandResult> results, List<String> capturedMessages) {
        JsonObject responseJson = new JsonObject();
        int acceptedCount = 0;
        int appliedCount = 0;
        int failedCount = 0;

        JsonArray commandResults = new JsonArray();
        for (int i = 0; i < results.size(); i++) {
            CommandResult result = results.get(i);
            if (result.isAccepted()) {
                acceptedCount++;
            }
            if (Boolean.TRUE.equals(result.getApplied())) {
                appliedCount++;
            }
            if (Boolean.FALSE.equals(result.getApplied())) {
                failedCount++;
            }

            JsonObject cmdResult = new JsonObject();
            cmdResult.addProperty("index", i);
            cmdResult.addProperty("command", result.getOriginalCommand());
            cmdResult.addProperty("status", result.getStatus());
            cmdResult.addProperty("accepted", result.isAccepted());
            if (result.getApplied() == null) {
                cmdResult.add("applied", JsonNull.INSTANCE);
            } else {
                cmdResult.addProperty("applied", result.getApplied());
            }
            cmdResult.addProperty("executionTimeMs", result.getExecutionTimeMs());
            cmdResult.addProperty("summary", result.getSummary());

            JsonArray perCommandMessages = new JsonArray();
            for (String chatMessage : result.getChatMessages()) {
                perCommandMessages.add(chatMessage);
            }
            cmdResult.add("chatMessages", perCommandMessages);
            commandResults.add(cmdResult);
        }

        responseJson.addProperty("totalCommands", totalCommands);
        responseJson.addProperty("acceptedCount", acceptedCount);
        responseJson.addProperty("appliedCount", appliedCount);
        responseJson.addProperty("failedCount", failedCount);
        responseJson.add("results", commandResults);

        JsonArray messages = new JsonArray();
        for (String message : capturedMessages) {
            messages.add(message);
        }
        responseJson.add("chatMessages", messages);

        responseJson.addProperty("hint", "Use get_blocks_in_area to verify the built structure and fix any issues.");
        return responseJson;
    }

    static JsonObject buildSafetyRejectedResponse(List<String> commands, int failedCommandIndex, String reason) {
        List<CommandResult> results = new ArrayList<>();
        String failedSummary = "Command rejected by safety validator: " + reason;
        String skippedSummary = "Skipped because safety validation failed at command " + (failedCommandIndex + 1);

        for (int i = 0; i < commands.size(); i++) {
            String summary = i == failedCommandIndex ? failedSummary : skippedSummary;
            results.add(CommandResult.builder()
                .accepted(false)
                .applied(false)
                .status("rejected_by_safety")
                .summary(summary)
                .chatMessages(List.of())
                .originalCommand(commands.get(i))
                .executionTimeMs(0L)
                .build());
        }

        JsonObject responseJson = buildExecuteCommandsResponse(commands.size(), results, List.of());
        responseJson.addProperty("hint", "Adjust commands to satisfy safety validation, then retry.");
        return responseJson;
    }
}
