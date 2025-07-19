package cuspymd.mcp.mod.command;

import com.google.gson.JsonArray;
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
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandExecutor.class);
    
    private final MCPConfig config;
    private final SafetyValidator safetyValidator;
    
    private static final Pattern BLOCKS_AFFECTED_PATTERN = Pattern.compile("(\\d+) blocks? (filled|affected|changed)");
    private static final Pattern ENTITIES_PATTERN = Pattern.compile("(\\d+) entities?");
    
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
                        JsonObject meta = new JsonObject();
                        meta.addProperty("failed_command_index", i);
                        meta.addProperty("failed_command", command);
                        meta.addProperty("total_commands", commands.size());
                        meta.addProperty("executed_commands", 0);
                        
                        return MCPProtocol.createErrorResponse(
                            "Command rejected by safety validator at command " + (i + 1) + ": " + validation.getErrorMessage(),
                            meta
                        );
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
        StringBuilder successMessage = new StringBuilder();
        successMessage.append("Executed ").append(commands.size()).append(" commands successfully:\n");
        
        for (int i = 0; i < commands.size(); i++) {
            String command = commands.get(i);
            try {
                CommandResult result = executeCommand(command).get(config.getServer().getRequestTimeoutMs(), TimeUnit.MILLISECONDS);
                results.add(result);
                
                if (!result.isSuccess()) {
                    JsonObject meta = new JsonObject();
                    meta.addProperty("failed_command_index", i);
                    meta.addProperty("failed_command", command);
                    meta.addProperty("total_commands", commands.size());
                    meta.addProperty("executed_commands", i);
                    
                    return MCPProtocol.createErrorResponse(
                        "Command execution failed at command " + (i + 1) + ": " + result.getMessage(),
                        meta
                    );
                }
                
                successMessage.append(i + 1).append(". ").append(command);
                if (result.getBlocksAffected() > 0) {
                    successMessage.append(" (").append(result.getBlocksAffected()).append(" blocks affected)");
                }
                if (result.getEntitiesAffected() > 0) {
                    successMessage.append(" (").append(result.getEntitiesAffected()).append(" entities affected)");
                }
                successMessage.append("\n");
                
            } catch (Exception e) {
                JsonObject meta = new JsonObject();
                meta.addProperty("failed_command_index", i);
                meta.addProperty("failed_command", command);
                meta.addProperty("total_commands", commands.size());
                meta.addProperty("executed_commands", i);
                
                return MCPProtocol.createErrorResponse(
                    "Command execution failed at command " + (i + 1) + ": " + e.getMessage(),
                    meta
                );
            }
        }
        
        return MCPProtocol.createSuccessResponse(successMessage.toString().trim());
    }
    
    private CompletableFuture<CommandResult> executeCommand(String command) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            
            MinecraftClient client = MinecraftClient.getInstance();
            ClientPlayerEntity player = client.player;
            
            if (player == null) {
                return CommandResult.builder()
                    .success(false)
                    .message("Player is not available")
                    .originalCommand(command)
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .build();
            }
            
            try {
                String fullCommand = command.startsWith("/") ? command : "/" + command;
                
                if (config.getClient().isLogCommands()) {
                    LOGGER.info("Executing command: {}", fullCommand);
                }
                
                client.execute(() -> {
                    if (client.getNetworkHandler() != null) {
                        client.getNetworkHandler().sendChatCommand(command);
                    }
                });
                
                Thread.sleep(100);
                
                return CommandResult.builder()
                    .success(true)
                    .message("Command executed successfully")
                    .originalCommand(command)
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .build();
                
            } catch (Exception e) {
                return CommandResult.builder()
                    .success(false)
                    .message("Failed to execute command: " + e.getMessage())
                    .originalCommand(command)
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .build();
            }
        });
    }
}