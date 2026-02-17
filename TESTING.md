# Testing Strategy for Minecraft MCP Server Mod

This document describes the testing approach for this mod and clarifies the distinction between Unit Tests and Integration Tests.

## Unit Tests

Unit tests are located in `src/test/java/` and are executed during the build process using `./gradlew test`.

### Coverage

- **Input Validation**: Ensuring that tool arguments (like coordinates) are complete and valid.
- **Timing Logic**: Verifying that deferred tasks (e.g., waiting for frames after teleportation) are executed after the correct number of ticks.
- **Data Processing**: Verifying that data transformation (e.g., Base64 encoding) is performed correctly.
- **Protocol Structure**: Ensuring that MCP response objects match the expected JSON schema.

### Limitations

Standard unit tests run in a headless environment without a running Minecraft instance. Therefore, they **cannot** test:
- Actual game screen capture (requires GPU and OpenGL context).
- Player teleportation within the game world.
- Interaction with live game objects (entities, blocks, world state).
- File IO dependent on `FabricLoader` paths.

## Integration Tests (Manual)

Due to the nature of Minecraft modding, core rendering and world-interaction features must be verified manually or through integration testing.

### Automated Integration Testing

Advanced integration testing can be performed using the [Fabric Game Test API](https://fabricmc.net/wiki/tutorial:gametest), which allows running tests within a minimized game instance. This is currently not implemented in the CI pipeline but can be added for future robustness.

### Manual Verification Steps

1. Launch the Minecraft client with the mod installed.
2. Connect an AI agent or use an MCP client (like Claude Desktop).
3. Call the `take_screenshot` tool with specific coordinates.
4. Verify that:
   - The player is teleported to the correct location.
   - The received image correctly reflects the view from those coordinates.
   - (If enabled) The image is saved to the `mcp_debug_screenshots/` folder.
