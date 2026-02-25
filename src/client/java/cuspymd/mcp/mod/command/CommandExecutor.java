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
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;

public class CommandExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandExecutor.class);
    private static final long COMMAND_MESSAGE_WAIT_MS = 700L;
    private static final long COMMAND_MESSAGE_IDLE_MS = 120L;
    private static final Set<String> TP_VERBS = Set.of("tp", "teleport");
    private static final Set<String> GIVE_VERBS = Set.of("give");
    private static final Set<String> FILL_VERBS = Set.of("fill");
    private static final Set<String> SET_BLOCK_VERBS = Set.of("setblock");
    private static final Set<String> SUMMON_VERBS = Set.of("summon");
    private static final Set<String> WEATHER_VERBS = Set.of("weather");
    private static final Set<String> TIME_VERBS = Set.of("time");
    private static final Set<String> CHAT_OUTPUT_VERBS = Set.of("say", "tell", "title");
    
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
            capture.drainAvailableCapturedMessages();
            for (String command : commands) {
                capture.drainAvailableCapturedMessages();
                long commandStartedAt = System.currentTimeMillis();
                CommandResult executionResult = executeCommandWithTimeout(command);
                List<ChatMessageCapture.CapturedMessage> capturedForCommand = collectMessagesForCommand(capture);
                List<ChatMessageCapture.CapturedMessage> commandWindowMessages =
                    keepMessagesAfter(commandStartedAt, capturedForCommand);
                List<String> commandMessages = toTextList(commandWindowMessages);
                List<String> analysisMessages = selectMessagesForOutcome(command, commandWindowMessages);
                allCapturedMessages.addAll(commandMessages);

                CommandResult analyzedResult =
                    applyOutcomeAnalysis(executionResult, analysisMessages, commandMessages);
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

    private List<ChatMessageCapture.CapturedMessage> collectMessagesForCommand(ChatMessageCapture capture) {
        List<ChatMessageCapture.CapturedMessage> messages = new ArrayList<>();
        long start = System.currentTimeMillis();
        long lastMessageAt = start;

        while (System.currentTimeMillis() - start < COMMAND_MESSAGE_WAIT_MS) {
            try {
                ChatMessageCapture.CapturedMessage message = capture.waitForCapturedMessage(75);
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

        messages.addAll(capture.drainAvailableCapturedMessages());
        return messages;
    }

    private CommandResult applyOutcomeAnalysis(
        CommandResult result,
        List<String> analysisMessages,
        List<String> chatMessages
    ) {
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
            CommandOutcomeAnalyzer.analyze(true, analysisMessages, result.getSummary());

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

    static List<String> selectMessagesForOutcome(
        String command,
        List<ChatMessageCapture.CapturedMessage> capturedMessages
    ) {
        if (capturedMessages == null || capturedMessages.isEmpty()) {
            return List.of();
        }

        String verb = extractCommandVerb(command);
        List<String> candidates = new ArrayList<>();

        for (ChatMessageCapture.CapturedMessage captured : capturedMessages) {
            if (captured == null || captured.text() == null || captured.text().isBlank()) {
                continue;
            }

            if (captured.source() == ChatMessageCapture.MessageSource.PLAYER_CHAT) {
                continue;
            }

            if (isLikelyCommandFeedback(verb, captured.text())) {
                candidates.add(captured.text());
            }
        }

        if (!candidates.isEmpty()) {
            return candidates;
        }

        List<String> fallback = new ArrayList<>();
        for (ChatMessageCapture.CapturedMessage captured : capturedMessages) {
            if (captured == null || captured.text() == null || captured.text().isBlank()) {
                continue;
            }

            if (captured.source() != ChatMessageCapture.MessageSource.PLAYER_CHAT
                && !CommandOutcomeAnalyzer.hasKnownOutcomeMarker(captured.text())) {
                fallback.add(captured.text());
            }
        }
        return fallback;
    }

    private static List<ChatMessageCapture.CapturedMessage> keepMessagesAfter(
        long timestampMs,
        List<ChatMessageCapture.CapturedMessage> messages
    ) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }

        List<ChatMessageCapture.CapturedMessage> filtered = new ArrayList<>();
        for (ChatMessageCapture.CapturedMessage message : messages) {
            if (message != null && message.timestampMs() >= timestampMs) {
                filtered.add(message);
            }
        }
        return filtered;
    }

    private static List<String> toTextList(List<ChatMessageCapture.CapturedMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }

        List<String> texts = new ArrayList<>();
        for (ChatMessageCapture.CapturedMessage message : messages) {
            if (message != null && message.text() != null) {
                texts.add(message.text());
            }
        }
        return texts;
    }

    private static boolean isLikelyCommandFeedback(String commandVerb, String message) {
        if (message == null || message.isBlank()) {
            return false;
        }

        if (CHAT_OUTPUT_VERBS.contains(commandVerb)) {
            return isExplicitCommandError(message);
        }

        if (CommandOutcomeAnalyzer.hasKnownOutcomeMarker(message)) {
            if (CommandOutcomeAnalyzer.hasFailureMarker(message)) {
                return true;
            }

            return successMarkerMatchesVerb(commandVerb, message);
        }

        return false;
    }

    private static boolean isExplicitCommandError(String message) {
        String normalized = message.toLowerCase(Locale.ROOT);
        return normalized.contains("<--[here]")
            || normalized.contains("unknown or incomplete command")
            || normalized.contains("unknown command")
            || normalized.contains("incorrect argument")
            || normalized.contains("expected");
    }

    private static boolean successMarkerMatchesVerb(String commandVerb, String message) {
        String normalizedMessage = message.toLowerCase(Locale.ROOT);
        if (commandVerb == null || commandVerb.isBlank()) {
            return CommandOutcomeAnalyzer.hasSuccessMarker(normalizedMessage);
        }

        if (TP_VERBS.contains(commandVerb)) {
            return normalizedMessage.contains("teleported");
        }
        if (GIVE_VERBS.contains(commandVerb)) {
            return normalizedMessage.contains("gave") || normalizedMessage.contains("given");
        }
        if (FILL_VERBS.contains(commandVerb)) {
            return normalizedMessage.contains("filled");
        }
        if (SET_BLOCK_VERBS.contains(commandVerb)) {
            return normalizedMessage.contains("set block")
                || normalizedMessage.contains("changed the block");
        }
        if (SUMMON_VERBS.contains(commandVerb)) {
            return normalizedMessage.contains("summoned");
        }
        if (WEATHER_VERBS.contains(commandVerb)) {
            return normalizedMessage.contains("set the weather");
        }
        if (TIME_VERBS.contains(commandVerb)) {
            return normalizedMessage.contains("set the time");
        }

        return CommandOutcomeAnalyzer.hasSuccessMarker(normalizedMessage);
    }

    private static String extractCommandVerb(String command) {
        if (command == null) {
            return "";
        }

        String trimmed = command.trim();
        if (trimmed.isEmpty()) {
            return "";
        }

        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }

        int spaceIndex = trimmed.indexOf(' ');
        String verb = spaceIndex >= 0 ? trimmed.substring(0, spaceIndex) : trimmed;
        return verb.toLowerCase(Locale.ROOT);
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
