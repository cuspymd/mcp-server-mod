package cuspymd.mcp.mod.command;

import cuspymd.mcp.mod.config.MCPConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

public class SafetyValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(SafetyValidator.class);
    
    private final MCPConfig config;
    
    private static final Pattern KILL_ALL_PATTERN = Pattern.compile("kill\\s+@[ae]");
    private static final Pattern CREATIVE_ALL_PATTERN = Pattern.compile("gamemode\\s+creative\\s+@a");
    private static final Pattern LARGE_COUNT_PATTERN = Pattern.compile("Count:(\\d+)");
    private static final Pattern FILL_COORDINATES_PATTERN = Pattern.compile("fill\\s+(-?\\d+)\\s+(-?\\d+)\\s+(-?\\d+)\\s+(-?\\d+)\\s+(-?\\d+)\\s+(-?\\d+)");
    
    public SafetyValidator(MCPConfig config) {
        this.config = config;
    }
    
    public ValidationResult validate(String command) {
        if (!config.getServer().isEnableSafety()) {
            return ValidationResult.success();
        }
        
        String normalizedCommand = command.toLowerCase().trim();
        
        if (normalizedCommand.startsWith("/")) {
            normalizedCommand = normalizedCommand.substring(1);
        }
        
        String[] parts = normalizedCommand.split("\\s+");
        if (parts.length == 0) {
            return ValidationResult.failure("Empty command");
        }
        
        String commandName = parts[0];
        
        if (!config.getServer().getAllowedCommands().contains(commandName)) {
            return ValidationResult.failure("Command '" + commandName + "' is not allowed");
        }
        
        if (KILL_ALL_PATTERN.matcher(normalizedCommand).find()) {
            return ValidationResult.failure("Potentially destructive pattern detected: mass entity killing");
        }
        
        if (CREATIVE_ALL_PATTERN.matcher(normalizedCommand).find() && 
            config.getSafety().isBlockCreativeForAll()) {
            return ValidationResult.failure("Setting creative mode for all players is not allowed");
        }
        
        var matcher = LARGE_COUNT_PATTERN.matcher(command);
        if (matcher.find()) {
            int count = Integer.parseInt(matcher.group(1));
            if (count > config.getSafety().getMaxEntitiesPerCommand()) {
                return ValidationResult.failure("Item/entity count (" + count + ") exceeds maximum allowed (" + 
                    config.getSafety().getMaxEntitiesPerCommand() + ")");
            }
        }
        
        if ("fill".equals(commandName)) {
            var fillMatcher = FILL_COORDINATES_PATTERN.matcher(command);
            if (fillMatcher.find()) {
                try {
                    int x1 = Integer.parseInt(fillMatcher.group(1));
                    int y1 = Integer.parseInt(fillMatcher.group(2));
                    int z1 = Integer.parseInt(fillMatcher.group(3));
                    int x2 = Integer.parseInt(fillMatcher.group(4));
                    int y2 = Integer.parseInt(fillMatcher.group(5));
                    int z2 = Integer.parseInt(fillMatcher.group(6));
                    
                    long volume = Math.abs((long)(x2 - x1 + 1) * (y2 - y1 + 1) * (z2 - z1 + 1));
                    if (volume > config.getSafety().getMaxBlocksPerCommand()) {
                        return ValidationResult.failure("Fill area volume (" + volume + ") exceeds maximum allowed (" + 
                            config.getSafety().getMaxBlocksPerCommand() + ")");
                    }
                } catch (NumberFormatException e) {
                    LOGGER.debug("Could not parse fill coordinates for validation", e);
                }
            }
        }
        
        return ValidationResult.success();
    }
    
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        
        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
        
        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }
        
        public static ValidationResult failure(String message) {
            return new ValidationResult(false, message);
        }
        
        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
    }
}