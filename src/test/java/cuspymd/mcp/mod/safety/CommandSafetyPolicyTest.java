package cuspymd.mcp.mod.safety;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CommandSafetyPolicyTest {

    @Test
    public void isHardDeniedNormalizesCaseAndSlash() {
        assertTrue(CommandSafetyPolicy.isHardDenied("OP"));
        assertTrue(CommandSafetyPolicy.isHardDenied("/deop"));
        assertTrue(CommandSafetyPolicy.isHardDenied(" whitelist "));
    }

    @Test
    public void filterAllowedCommandsRemovesHardDeniedAndNormalizes() {
        List<String> filtered = CommandSafetyPolicy.filterAllowedCommands(
            List.of(" TP ", "/op", "fill 1 2 3 4 5 6 stone", "tp", "Reload")
        );

        assertEquals(List.of("tp", "fill"), filtered);
    }
}
