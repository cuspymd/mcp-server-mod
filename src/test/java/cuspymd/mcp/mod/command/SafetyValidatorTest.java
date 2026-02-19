package cuspymd.mcp.mod.command;

import com.google.gson.Gson;
import cuspymd.mcp.mod.config.MCPConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SafetyValidatorTest {
    private static final Gson GSON = new Gson();

    @Test
    public void hardDenyCommandIsRejectedEvenWhenListedInAllowedCommands() {
        MCPConfig config = GSON.fromJson("""
            {
              "server": {
                "enableSafety": true,
                "allowedCommands": ["op", "tp"]
              }
            }
            """, MCPConfig.class);

        SafetyValidator validator = new SafetyValidator(config);
        SafetyValidator.ValidationResult result = validator.validate("op @a");

        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("hard safety policy"));
    }

    @Test
    public void commandInFilteredAllowListIsAccepted() {
        MCPConfig config = GSON.fromJson("""
            {
              "server": {
                "enableSafety": true,
                "allowedCommands": ["/tp", "reload"]
              }
            }
            """, MCPConfig.class);

        SafetyValidator validator = new SafetyValidator(config);
        SafetyValidator.ValidationResult result = validator.validate("tp @s ~ ~ ~");

        assertTrue(result.isValid());
    }
}
