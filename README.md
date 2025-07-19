# Minecraft MCP Server Mod

A Fabric mod that implements a Model Context Protocol (MCP) server, enabling AI assistants like Claude to interact with Minecraft through structured commands.

## Overview

This mod creates an HTTP server within the Minecraft client that accepts MCP protocol requests, allowing Large Language Models to execute Minecraft commands safely and efficiently. The mod includes comprehensive safety validation to prevent destructive operations.

## Features

- **MCP Protocol Support**: Full implementation of Model Context Protocol for AI interaction
- **Safety Validation**: Comprehensive command filtering and validation system
- **Asynchronous Execution**: Non-blocking command execution to maintain game performance
- **Configurable Settings**: Customizable safety limits, server settings, and command permissions
- **Real-time Feedback**: Detailed execution results including block counts and entity information

## Requirements

- **Minecraft**: 1.21.4
- **Fabric Loader**: 0.16.14 or higher
- **Fabric API**: 0.119.3+1.21.4
- **Java**: 21 or higher

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for Minecraft 1.21.4
2. Download and install [Fabric API](https://modrinth.com/mod/fabric-api)
3. Place the mod JAR file in your `mods` folder
4. Launch Minecraft with the Fabric profile

## Usage

### Starting the MCP Server

The MCP server starts automatically when you launch Minecraft with the mod installed. By default, it runs on `localhost:8080`.

### Configuration

The mod creates a configuration file at `config/mcp-client.json`:

```json
{
  "server": {
    "port": 8080,
    "host": "localhost",
    "enable_safety": true,
    "max_area_size": 50,
    "allowed_commands": ["fill", "clone", "setblock", "summon", "tp", "give"],
    "request_timeout_ms": 30000
  },
  "client": {
    "auto_start": true,
    "show_notifications": true,
    "log_level": "INFO",
    "log_commands": false
  },
  "safety": {
    "max_entities_per_command": 10,
    "max_blocks_per_command": 125000,
    "block_creative_for_all": true,
    "require_op_for_admin_commands": true
  }
}
```

### Connecting with AI Assistants

Connect your AI assistant (like Claude) to the MCP server using the endpoint:
```
http://localhost:8080/mcp
```

The server supports the `execute_commands` tool for running Minecraft commands.

### Example Commands

The AI can execute commands like:
- `fill ~ ~ ~ ~10 ~5 ~8 oak_planks` - Fill an area with blocks
- `summon villager ~ ~ ~` - Spawn entities
- `setblock ~ ~1 ~ oak_door` - Place specific blocks
- `tp @s ~ ~10 ~` - Teleport players
- `give @s diamond_sword` - Give items

## Safety Features

### Allowed Commands
- Building: `fill`, `clone`, `setblock`
- Entities: `summon`, `tp`, `teleport`
- Items: `give`
- Game state: `gamemode`, `effect`, `enchant`, `weather`, `time`
- Communication: `say`, `tell`, `title`

### Blocked Operations
- Mass entity destruction (`kill @a`, `kill @e`)
- Excessive area operations (>50×50×50 blocks)
- Mass item generation (>100 items)
- Global creative mode assignment

## Development

### Building

```bash
./gradlew build
```

### Running in Development

```bash
./gradlew runClient
```

### Project Structure

```
src/
├── main/java/cuspymd/mcp/mod/
│   ├── MCPServerMod.java           # Main mod class
│   ├── MCPServerModClient.java     # Client initializer
│   ├── server/                     # MCP server implementation
│   ├── command/                    # Command execution system
│   ├── config/                     # Configuration management
│   └── utils/                      # Utility classes
└── main/resources/
    ├── fabric.mod.json             # Mod metadata
    └── *.mixins.json              # Mixin configurations
```

## API Reference

### MCP Endpoints

- `POST /mcp/initialize` - Initialize MCP session
- `POST /mcp/ping` - Health check
- `POST /mcp/tools/list` - List available tools
- `POST /mcp/tools/call` - Execute commands

### Tool: execute_commands

Execute one or more Minecraft commands sequentially with safety validation.

**Parameters:**
- `commands` (array): List of Minecraft commands (without leading slash)
- `validate_safety` (boolean): Enable safety validation (default: true)

**Example Request:**
```json
{
  "method": "tools/call",
  "params": {
    "name": "execute_commands",
    "arguments": {
      "commands": [
        "fill ~ ~ ~ ~10 ~5 ~8 oak_planks",
        "setblock ~5 ~6 ~4 oak_door"
      ],
      "validate_safety": true
    }
  }
}
```

## License

This project is licensed under the CC0-1.0 License.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## Support

For issues and questions:
- Check the [Issues](https://github.com/your-repo/issues) page
- Review the configuration documentation
- Enable debug logging for detailed troubleshooting