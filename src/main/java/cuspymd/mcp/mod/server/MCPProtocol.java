package cuspymd.mcp.mod.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import cuspymd.mcp.mod.config.MCPConfig;

public class MCPProtocol {
    
    public static JsonArray getToolsListResponse(MCPConfig config) {
        JsonArray tools = new JsonArray();
        
        // Execute commands tool
        JsonObject executeCommandsTool = new JsonObject();
        executeCommandsTool.addProperty("name", "execute_commands");
        executeCommandsTool.addProperty("description",
            "Execute one or more Minecraft commands sequentially. " +
            "Allowed commands: fill, clone, setblock, summon, tp, give, gamemode, effect, enchant, weather, time, say, tell, title.\n\n" +
            "BLOCK STATE SYNTAX (critical for quality builds):\n" +
            "- Doors: setblock X Y Z oak_door[facing=north,half=lower,hinge=left,open=false] then setblock X Y+1 Z oak_door[facing=north,half=upper,hinge=left,open=false]\n" +
            "- Stairs: setblock X Y Z oak_stairs[facing=east,half=bottom,shape=straight]\n" +
            "- Slabs: setblock X Y Z oak_slab[type=top] or [type=bottom] or [type=double]\n" +
            "- Trapdoors: setblock X Y Z oak_trapdoor[facing=north,half=top,open=false]\n" +
            "- Fences/Walls: placed adjacently they auto-connect\n" +
            "- Logs/Pillars: setblock X Y Z oak_log[axis=y] (y=vertical, x/z=horizontal)\n" +
            "- Glazed Terracotta: [facing=north/south/east/west]\n" +
            "- Beds: setblock X Y Z red_bed[facing=south,part=foot] then setblock X Y Z+1 red_bed[facing=south,part=head] (head goes in facing direction: south=+Z, north=-Z, east=+X, west=-X)\n" +
            "- Chests: setblock X Y Z chest[facing=north]\n" +
            "- Torches: torch (floor), wall_torch[facing=north] (wall)\n" +
            "- Lanterns: lantern[hanging=true/false]\n" +
            "- Glass Panes: auto-connect to adjacent blocks\n\n" +
            "FILL COMMAND SYNTAX:\n" +
            "- fill X1 Y1 Z1 X2 Y2 Z2 <block> [replace|hollow|outline|destroy|keep]\n" +
            "- 'hollow' fills outer shell with block, inner with air - great for rooms\n" +
            "- 'outline' fills only outer shell, keeps interior unchanged\n" +
            "- 'replace <oldBlock>' replaces only matching blocks\n" +
            "- fill X1 Y1 Z1 X2 Y2 Z2 air replace <block> - removes specific block type\n\n" +
            "BUILDING BEST PRACTICES:\n" +
            "1. Use get_player_info first to get your position and use ABSOLUTE coordinates (not relative ~)\n" +
            "2. Plan structure dimensions before building. Typical house: 7-11 wide, 5-7 tall, 9-13 deep\n" +
            "3. Build in order: foundation -> walls (use fill hollow) -> roof -> windows/doors -> interior -> decoration\n" +
            "4. Doors MUST have two blocks: lower half (half=lower) and upper half (half=upper) at Y+1\n" +
            "5. Windows: use glass_pane, not glass (panes look much better)\n" +
            "6. Roofs: use stairs with correct facing for sloped roofs, slabs for flat roofs\n" +
            "7. After building, ALWAYS verify with get_blocks_in_area to check for errors\n" +
            "8. Group related commands in one call (e.g., all wall commands together) for efficiency\n" +
            "9. Max fill volume: 32768 blocks per command (Minecraft limit). Max entities per summon: 10"
        );

        JsonObject inputSchema = new JsonObject();
        inputSchema.addProperty("type", "object");

        JsonObject properties = new JsonObject();

        JsonObject commandsProperty = new JsonObject();
        commandsProperty.addProperty("type", "array");
        commandsProperty.addProperty("description", "Array of Minecraft commands to execute without leading slash. Each command is executed sequentially. The response includes per-command results showing success/failure and any chat feedback from the server.");
        commandsProperty.addProperty("minItems", 1);
        
        JsonObject commandsItems = new JsonObject();
        commandsItems.addProperty("type", "string");
        commandsProperty.add("items", commandsItems);
        
        JsonObject validateSafetyProperty = new JsonObject();
        validateSafetyProperty.addProperty("type", "boolean");
        validateSafetyProperty.addProperty("description", "Whether to validate command safety (default: true)");
        validateSafetyProperty.addProperty("default", true);
        
        properties.add("commands", commandsProperty);
        properties.add("validate_safety", validateSafetyProperty);
        inputSchema.add("properties", properties);
        
        JsonArray required = new JsonArray();
        required.add("commands");
        inputSchema.add("required", required);
        
        executeCommandsTool.add("inputSchema", inputSchema);
        tools.add(executeCommandsTool);
        
        // Get player info tool
        JsonObject getPlayerInfoTool = new JsonObject();
        getPlayerInfoTool.addProperty("name", "get_player_info");
        getPlayerInfoTool.addProperty("description",
            "Get current player position and world context. CALL THIS FIRST before any building task to get absolute coordinates.\n\n" +
            "Returns:\n" +
            "- blockPosition: {x,y,z} integer coordinates - USE THESE for setblock/fill commands\n" +
            "- position: {x,y,z} exact floating-point coordinates\n" +
            "- facingDirection: cardinal direction (North/South/East/West)\n" +
            "- frontPosition: {x,y,z} 3 blocks ahead of player - good starting point for builds\n" +
            "- gameMode, dimension, timeOfDay, health, foodLevel, inventory\n\n" +
            "IMPORTANT: Minecraft Y-axis is vertical (Y=64 is typical ground level). " +
            "Use blockPosition for command coordinates. " +
            "Build at frontPosition or offset from blockPosition using absolute coordinates for reliability."
        );
        
        JsonObject playerInfoInputSchema = new JsonObject();
        playerInfoInputSchema.addProperty("type", "object");
        
        JsonObject playerInfoProperties = new JsonObject();
        // No required parameters for this tool
        playerInfoInputSchema.add("properties", playerInfoProperties);
        
        getPlayerInfoTool.add("inputSchema", playerInfoInputSchema);
        tools.add(getPlayerInfoTool);
        
        // Get blocks in area tool
        JsonObject getBlocksInAreaTool = new JsonObject();
        getBlocksInAreaTool.addProperty("name", "get_blocks_in_area");
        int maxAreaSize = config != null ? config.getServer().getMaxAreaSize() : 48;
        getBlocksInAreaTool.addProperty("description",
            "Scan and return all non-air blocks in a rectangular area. Use this to VERIFY builds after construction.\n\n" +
            "Maximum " + maxAreaSize + " blocks per axis. Air blocks are excluded. " +
            "Returns compressed block data grouped by type with regions (connected areas) and single blocks.\n\n" +
            "USAGE: After building, scan the build area to verify:\n" +
            "- All walls are complete (no gaps)\n" +
            "- Doors have both upper and lower halves\n" +
            "- Roof is fully covered\n" +
            "- Windows are placed correctly\n" +
            "If you find errors, use execute_commands to fix them."
        );
        
        JsonObject blocksInputSchema = new JsonObject();
        blocksInputSchema.addProperty("type", "object");
        
        JsonObject blocksProperties = new JsonObject();
        
        // From position
        JsonObject fromProperty = new JsonObject();
        fromProperty.addProperty("type", "object");
        fromProperty.addProperty("description", "Starting position of the area to scan");
        JsonObject fromPosProperties = new JsonObject();
        JsonObject xProp = new JsonObject();
        xProp.addProperty("type", "integer");
        JsonObject yProp = new JsonObject();
        yProp.addProperty("type", "integer");
        JsonObject zProp = new JsonObject();
        zProp.addProperty("type", "integer");
        fromPosProperties.add("x", xProp);
        fromPosProperties.add("y", yProp);
        fromPosProperties.add("z", zProp);
        fromProperty.add("properties", fromPosProperties);
        JsonArray fromRequired = new JsonArray();
        fromRequired.add("x");
        fromRequired.add("y");
        fromRequired.add("z");
        fromProperty.add("required", fromRequired);
        
        // To position
        JsonObject toProperty = new JsonObject();
        toProperty.addProperty("type", "object");
        toProperty.addProperty("description", "Ending position of the area to scan");
        JsonObject toPosProperties = new JsonObject();
        toPosProperties.add("x", xProp);
        toPosProperties.add("y", yProp);
        toPosProperties.add("z", zProp);
        toProperty.add("properties", toPosProperties);
        JsonArray toRequired = new JsonArray();
        toRequired.add("x");
        toRequired.add("y");
        toRequired.add("z");
        toProperty.add("required", toRequired);
        
        blocksProperties.add("from", fromProperty);
        blocksProperties.add("to", toProperty);
        blocksInputSchema.add("properties", blocksProperties);
        
        JsonArray blocksRequiredFields = new JsonArray();
        blocksRequiredFields.add("from");
        blocksRequiredFields.add("to");
        blocksInputSchema.add("required", blocksRequiredFields);
        
        getBlocksInAreaTool.add("inputSchema", blocksInputSchema);
        tools.add(getBlocksInAreaTool);
        
        return tools;
    }
    
    public static JsonObject createSuccessResponse(String message) {
        JsonObject response = new JsonObject();
        response.addProperty("isError", false);
        
        JsonArray content = new JsonArray();
        JsonObject textContent = new JsonObject();
        textContent.addProperty("type", "text");
        textContent.addProperty("text", message);
        content.add(textContent);
        
        response.add("content", content);
        return response;
    }
    
    public static JsonObject createErrorResponse(String message, JsonObject meta) {
        JsonObject response = new JsonObject();
        response.addProperty("isError", true);
        
        JsonArray content = new JsonArray();
        JsonObject textContent = new JsonObject();
        textContent.addProperty("type", "text");
        textContent.addProperty("text", message);
        content.add(textContent);
        
        response.add("content", content);
        if (meta != null) {
            response.add("_meta", meta);
        }
        
        return response;
    }
}