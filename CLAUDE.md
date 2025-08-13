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
./gradlew test                 # Run unit tests only
```

## Architecture Overview

### Core Components
- **MCPServerModClient**: Client-side entry point that starts the HTTP MCP server
- **HTTPMCPServer**: HTTP server implementation (port 8080) that handles MCP protocol
- **CommandExecutor**: Executes Minecraft commands with safety validation and result capture
- **SafetyValidator**: Validates commands against security rules and size limits
- **BlockCompressor**: Optimizes block change representations by grouping connected regions
- **BlockScanner**: Scans world areas to detect block changes and structures
- **MCPConfig**: Configuration management for server settings and safety rules

### Package Structure
The mod uses Fabric's split environment feature with separate client and main source sets:

```
src/
├── main/java/cuspymd/mcp/mod/   # Server-side code
│   ├── MCPServerMod.java        # Main mod class
│   ├── config/MCPConfig.java    # Configuration management
│   ├── server/MCPProtocol.java  # MCP protocol definitions
│   └── bridge/IPCClient.java    # Inter-process communication
├── client/java/cuspymd/mcp/mod/ # Client-side code
│   ├── MCPServerModClient.java  # Client mod initializer
│   ├── bridge/HTTPMCPServer.java # HTTP server implementation
│   ├── command/                 # Command execution system
│   │   ├── CommandExecutor.java
│   │   ├── SafetyValidator.java
│   │   ├── CommandResult.java
│   │   └── ChatMessageCapture.java
│   └── utils/                   # Utility classes
│       ├── BlockCompressor.java
│       ├── BlockScanner.java
│       ├── CommandParser.java
│       ├── CoordinateUtils.java
│       └── PlayerInfoProvider.java
└── test/java/cuspymd/mcp/mod/   # Unit tests
    └── BlockCompressorTest.java
```

## MCP Protocol Implementation

The mod implements the MCP (Model Context Protocol) with these endpoints:
- `POST /mcp/initialize` - Session initialization
- `POST /mcp/ping` - Connection health check
- `POST /mcp/tools/list` - List available tools
- `POST /mcp/tools/call` - Execute the `execute_commands` tool

### Primary Tool: execute_commands
Executes Minecraft commands with comprehensive safety validation. Commands are executed sequentially and results include:
- Block changes with compressed representations for connected regions
- Entity spawns and modifications
- Chat messages captured during execution
- Player position and dimension information
- Execution timing and success status

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

### Architecture Patterns
- **Split Environment**: Uses Fabric's split environment feature with separate client/main source sets
- **Client-focused**: The HTTP MCP server runs entirely in the Minecraft client
- **Async Execution**: Commands executed asynchronously to prevent main thread blocking
- **Result Compression**: Block changes are automatically compressed using connected component analysis

### Testing
- Unit tests use JUnit 5 (`org.junit.jupiter`)
- Tests focus on utility classes like `BlockCompressor` 
- Test coverage includes edge cases like L-shaped regions and vertical towers

### Key Implementation Details
- Commands are executed through Minecraft's built-in command system
- Chat messages are captured via Mixin injection during command execution
- Block changes are detected and compressed into regions vs. single blocks
- All command execution includes comprehensive safety validation
- Configuration auto-loads at client initialization and supports runtime modification
- Log output uses SLF4J with mod ID "mcp-server-mod"