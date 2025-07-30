package cuspymd.mcp.mod.server;

import java.util.List;

public class MCPProtocol {
    
    public record ToolSchema(
        String type,
        ToolSchemaProperties properties,
        List<String> required
    ) {}
    
    public record ToolSchemaProperties(
        CommandsProperty commands,
        ValidateSafetyProperty validate_safety
    ) {}
    
    public record CommandsProperty(
        String type,
        String description,
        int minItems,
        CommandsItems items
    ) {}
    
    public record CommandsItems(
        String type
    ) {}
    
    public record ValidateSafetyProperty(
        String type,
        String description,
        boolean default_
    ) {}
    
    public record Tool(
        String name,
        String description,
        ToolSchema inputSchema
    ) {}
    
    public record TextContent(
        String type,
        String text
    ) {}
    
    public record MCPResponse(
        boolean isError,
        List<TextContent> content,
        Object _meta
    ) {
        public MCPResponse(boolean isError, List<TextContent> content) {
            this(isError, content, null);
        }
    }
    
    public static List<Tool> getToolsList() {
        Tool executeCommandsTool = new Tool(
            "execute_commands",
            "Execute one or more Minecraft commands sequentially",
            new ToolSchema(
                "object",
                new ToolSchemaProperties(
                    new CommandsProperty(
                        "array",
                        "Array of Minecraft commands to execute (without leading slash)",
                        1,
                        new CommandsItems("string")
                    ),
                    new ValidateSafetyProperty(
                        "boolean",
                        "Whether to validate command safety (default: true)",
                        true
                    )
                ),
                List.of("commands")
            )
        );
        
        return List.of(executeCommandsTool);
    }
    
    public static MCPResponse createSuccessResponse(String message) {
        return new MCPResponse(
            false,
            List.of(new TextContent("text", message))
        );
    }
    
    public static MCPResponse createErrorResponse(String message, Object meta) {
        return new MCPResponse(
            true,
            List.of(new TextContent("text", message)),
            meta
        );
    }
}