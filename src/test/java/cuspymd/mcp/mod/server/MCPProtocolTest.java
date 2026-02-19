package cuspymd.mcp.mod.server;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import cuspymd.mcp.mod.config.MCPConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MCPProtocolTest {
    private static final Gson GSON = new Gson();

    @Test
    public void executeCommandsDescriptionUsesFilteredAllowList() {
        MCPConfig config = GSON.fromJson("""
            {
              "server": {
                "allowedCommands": ["tp", "op", "/fill", "reload"]
              }
            }
            """, MCPConfig.class);

        JsonArray tools = MCPProtocol.getToolsListResponse(config);
        JsonObject executeCommandsTool = tools.get(0).getAsJsonObject();
        String description = executeCommandsTool.get("description").getAsString();
        String allowedLine = description.split("\\n\\n", 2)[0];

        assertTrue(allowedLine.contains("Allowed commands: tp, fill."));
        assertFalse(allowedLine.contains(" op"));
        assertFalse(allowedLine.contains("reload"));
    }
}
