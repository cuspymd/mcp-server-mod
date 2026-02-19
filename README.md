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
    "log_commands": false,
    "save_screenshots_for_debug": false
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

The server supports three main tools:
- `execute_commands` - Execute Minecraft commands with safety validation
- `get_player_info` - Get comprehensive player information
- `get_blocks_in_area` - Scan and retrieve blocks in a specified area
- `take_screenshot` - Capture game screen with optional camera control

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

### Tool: get_player_info

Get comprehensive player information including position, facing direction, health, inventory, and game state.

**Parameters:** None required

**Response includes:**
- Exact position (x, y, z coordinates) and block coordinates
- Facing direction (yaw, pitch, cardinal direction)  
- Calculated front position for building (3 blocks ahead)
- Look vector for directional calculations
- Health, food, and experience status
- Current game mode and dimension
- World time information
- Inventory details (selected slot, main/off-hand items)

**Example Request:**
```json
{
  "method": "tools/call",
  "params": {
    "name": "get_player_info",
    "arguments": {}
  }
}
```

### Tool: get_blocks_in_area

Scan and retrieve all non-air blocks within a specified rectangular area. Useful for analyzing structures or checking build areas.

**Parameters:**
- `from` (object): Starting position with x, y, z coordinates
- `to` (object): Ending position with x, y, z coordinates

**Response includes:**
- List of all non-air blocks in the area
- Block types and positions
- Total block count
- Area dimensions and validation info

**Example Request:**
```json
{
  "method": "tools/call", 
  "params": {
    "name": "get_blocks_in_area",
    "arguments": {
      "from": {"x": 100, "y": 64, "z": 200},
      "to": {"x": 110, "y": 74, "z": 210}
    }
  }
}
```

**Note:** Maximum area size per axis is limited by server configuration (default: 50 blocks).

### Tool: take_screenshot

Capture a screenshot of the current Minecraft game screen. Optionally, you can specify coordinates and rotation to move the player and set their gaze before taking the screenshot.

**Parameters:**
- `x` (number, optional): X coordinate to teleport the player to.
- `y` (number, optional): Y coordinate to teleport the player to.
- `z` (number, optional): Z coordinate to teleport the player to.
- `yaw` (number, optional): Yaw rotation for horizontal view (accepts Minecraft yaw values, e.g. `-180..180` or `0..360`).
- `pitch` (number, optional): Pitch rotation for vertical view (typical range `-90..90`).

`take_screenshot` applies movement/rotation via server-authoritative `/tp` commands.
If `x`, `y`, and `z` are provided, the tool sends `tp @s ...` and waits for server position sync before capture.
If only `yaw` and/or `pitch` are provided, the tool sends `tp @s ~ ~ ~ <yaw> <pitch>` and waits for rotation sync before capture (unspecified axis keeps current value).
If teleport permission is missing, the tool returns an error.

**Response includes:**
- Base64 encoded PNG image data.
- MIME type (`image/png`).

**Example Request:**
```json
{
  "method": "tools/call",
  "params": {
    "name": "take_screenshot",
    "arguments": {
      "x": 120.5,
      "y": 70,
      "z": -200.5,
      "yaw": 180,
      "pitch": 0
    }
  }
}
```

## Debugging

### Local Screenshot Storage

For debugging purposes, you can enable local saving of every screenshot captured by the MCP server.

1. Open `config/mcp-client.json`.
2. Set `"save_screenshots_for_debug": true` in the `client` section.
3. Screenshots will be saved to the `mcp_debug_screenshots/` directory in your Minecraft instance folder.
4. Files are named using the pattern: `screenshot_YYYYMMDD_HHMMSS_SSS.png`.

## License

This project is licensed under the CC0-1.0 License.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly (See [TESTING.md](TESTING.md) for more info)
5. Submit a pull request

## Support

For issues and questions:
- Check the [Issues](https://github.com/your-repo/issues) page
- Review the configuration documentation
- Enable debug logging for detailed troubleshooting
