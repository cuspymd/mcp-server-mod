package cuspymd.mcp.mod.command;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CommandExecutorResponseSchemaTest {

    @Test
    public void responseIncludesCountsAndPerCommandMessages() {
        CommandResult applied = CommandResult.builder()
            .accepted(true)
            .applied(true)
            .status("applied")
            .summary("Filled 4 blocks")
            .chatMessages(List.of("Successfully filled 4 block(s)"))
            .originalCommand("fill 0 0 0 1 0 1 stone")
            .executionTimeMs(50)
            .build();

        CommandResult rejected = CommandResult.builder()
            .accepted(true)
            .applied(false)
            .status("rejected_by_game")
            .summary("Cannot enchant")
            .chatMessages(List.of("Carrot cannot support that enchantment"))
            .originalCommand("enchant @s minecraft:unbreaking 1")
            .executionTimeMs(52)
            .build();

        JsonObject payload = CommandExecutor.buildExecuteCommandsResponse(
            2,
            List.of(applied, rejected),
            List.of("Successfully filled 4 block(s)", "Carrot cannot support that enchantment")
        );

        assertEquals(2, payload.get("totalCommands").getAsInt());
        assertEquals(2, payload.get("acceptedCount").getAsInt());
        assertEquals(1, payload.get("appliedCount").getAsInt());
        assertEquals(1, payload.get("failedCount").getAsInt());

        JsonObject first = payload.getAsJsonArray("results").get(0).getAsJsonObject();
        assertEquals("applied", first.get("status").getAsString());
        assertTrue(first.get("accepted").getAsBoolean());
        assertTrue(first.get("applied").getAsBoolean());

        JsonObject second = payload.getAsJsonArray("results").get(1).getAsJsonObject();
        assertEquals("rejected_by_game", second.get("status").getAsString());
        assertTrue(second.get("accepted").getAsBoolean());
        assertFalse(second.get("applied").getAsBoolean());
        assertEquals(1, second.getAsJsonArray("chatMessages").size());
    }

    @Test
    public void safetyRejectedResponseUsesPerCommandSchema() {
        JsonObject payload = CommandExecutor.buildSafetyRejectedResponse(
            List.of("say ok", "kill @s", "give @s dirt 1"),
            1,
            "Command 'kill' is not allowed"
        );

        assertEquals(3, payload.get("totalCommands").getAsInt());
        assertEquals(0, payload.get("acceptedCount").getAsInt());
        assertEquals(0, payload.get("appliedCount").getAsInt());
        assertEquals(3, payload.get("failedCount").getAsInt());

        JsonObject first = payload.getAsJsonArray("results").get(0).getAsJsonObject();
        assertEquals("rejected_by_safety", first.get("status").getAsString());
        assertFalse(first.get("accepted").getAsBoolean());
        assertFalse(first.get("applied").getAsBoolean());

        JsonObject second = payload.getAsJsonArray("results").get(1).getAsJsonObject();
        assertEquals("rejected_by_safety", second.get("status").getAsString());
        assertTrue(second.get("summary").getAsString().contains("Command rejected by safety validator"));
        assertTrue(second.get("summary").getAsString().contains("kill"));

        JsonObject third = payload.getAsJsonArray("results").get(2).getAsJsonObject();
        assertEquals("rejected_by_safety", third.get("status").getAsString());
        assertTrue(third.get("summary").getAsString().contains("Skipped because safety validation failed at command 2"));

        assertNotNull(payload.get("hint"));
    }
}
