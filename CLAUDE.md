# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Minecraft Fabric mod that implements an MCP (Model Context Protocol) server, allowing LLMs like Claude to execute Minecraft commands through HTTP requests. The mod runs on Fabric 1.21.4 and creates an HTTP server on port 8080 that accepts MCP protocol messages.

## Development Commands

### Building the Mod
```bash
./gradlew build                 # Build the mod JAR
./gradlew clean build          # Clean build from scratch
```

### Running in Development
```bash
./gradlew runClient            # Run Minecraft client with the mod loaded
./gradlew runServer            # Run Minecraft server with the mod loaded
```

### Code Quality
```bash
./gradlew check                # Run all checks and tests
```

## Architecture Overview

### Core Components
- **MCPServerModClient**: Client-side entry point that starts the MCP HTTP server
- **MCPServer**: HTTP server implementation (port 8080) that handles MCP protocol
- **CommandExecutor**: Executes Minecraft commands with safety validation
- **SafetyValidator**: Validates commands against security rules
- **MCPConfig**: Configuration management for server settings and safety rules

### Package Structure
```
cuspymd.mcp.mod/
├── MCPServerMod.java           # Main mod class (server-side)
├── MCPServerModClient.java     # Client mod initializer
├── server/                     # MCP HTTP server implementation
│   ├── MCPServer.java
│   ├── MCPRequestHandler.java
│   └── MCPProtocol.java
├── command/                    # Command execution system
│   ├── CommandExecutor.java
│   ├── SafetyValidator.java
│   └── CommandResult.java
├── config/                     # Configuration management
│   └── MCPConfig.java
└── utils/                      # Utility classes
    ├── CommandParser.java
    └── CoordinateUtils.java
```

## MCP Protocol Implementation

The mod implements the MCP (Model Context Protocol) with these endpoints:
- `POST /mcp/initialize` - Session initialization
- `POST /mcp/ping` - Connection health check
- `POST /mcp/tools/list` - List available tools
- `POST /mcp/tools/call` - Execute the `execute_commands` tool

### Primary Tool: execute_commands
Executes Minecraft commands with safety validation. Commands are executed sequentially and results include affected block counts and entity spawns.

## Safety System

The mod includes comprehensive safety validation:
- **Allowed commands**: fill, clone, setblock, summon, tp, give, gamemode, effect, enchant, weather, time, say
- **Blocked patterns**: 
  - Mass entity killing (`kill @a`, `kill @e`)
  - Large area operations (>50x50x50 blocks)
  - Mass item/entity creation (>100 count)
  - Creative mode for all players

## Configuration

The mod uses `config/mcp-client.json` for configuration:
- Server settings (port, host, timeouts)
- Safety limits (max entities, max blocks)
- Client preferences (auto-start, logging)

Configuration is loaded at client initialization and can be modified at runtime.

## Key Technical Details

- **Fabric Loader**: 0.16.14+
- **Minecraft Version**: 1.21.4
- **Java Version**: 21+
- **Mixins**: Client-side mixins for game integration
- **HTTP Server**: Java NIO-based HTTP server
- **JSON Processing**: Uses Gson (Minecraft built-in)
- **Async Processing**: Commands executed asynchronously to prevent main thread blocking

## Development Notes

- The mod is client-side focused - the HTTP server runs in the client
- Commands are executed through Minecraft's command system
- All command execution is validated for safety before execution
- The mod auto-starts the MCP server when the client initializes (configurable)
- Log output uses SLF4J with mod ID "mcp-server-mod"