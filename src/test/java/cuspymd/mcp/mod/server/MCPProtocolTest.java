package cuspymd.mcp.mod.server;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import cuspymd.mcp.mod.config.MCPConfig;
import java.util.stream.IntStream;
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
        assertTrue(description.contains("acceptedCount"));
        assertTrue(description.contains("status values: applied, rejected_by_game, execution_error, timed_out, rejected_by_safety, unknown"));
    }

    @Test
    public void toolsListIncludesTakeScreenshot() {
        MCPConfig config = GSON.fromJson("{\"server\":{}}", MCPConfig.class);
        JsonArray tools = MCPProtocol.getToolsListResponse(config);

        boolean hasTakeScreenshot = IntStream.range(0, tools.size())
            .mapToObj(i -> tools.get(i).getAsJsonObject())
            .anyMatch(tool -> "take_screenshot".equals(tool.get("name").getAsString()));

        assertTrue(hasTakeScreenshot);
    }

    @Test
    public void toolsListCanExcludeTakeScreenshot() {
        MCPConfig config = GSON.fromJson("{\"server\":{}}", MCPConfig.class);
        JsonArray tools = MCPProtocol.getToolsListResponse(config, false);

        boolean hasTakeScreenshot = IntStream.range(0, tools.size())
            .mapToObj(i -> tools.get(i).getAsJsonObject())
            .anyMatch(tool -> "take_screenshot".equals(tool.get("name").getAsString()));

        assertFalse(hasTakeScreenshot);
    }
}
