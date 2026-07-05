# Minecraft 26.1 Migration Notes

## Summary

This branch ports `mcp-server-mod` from Minecraft `1.21.11` with Fabric Yarn mappings to Minecraft `26.1` with Mojang official/unobfuscated names.

Minecraft `26.1` is the first unobfuscated release line targeted by this project. Fabric no longer officially supports Yarn mappings for `26.1+`, so the port uses Mojang names directly and removes the Gradle `mappings` dependency.

## Key build changes

- Gradle wrapper: `9.5.1`
- Loom plugin id: `net.fabricmc.fabric-loom`
- Minecraft: `26.1`
- Fabric Loader: `0.19.3+`
- Fabric API: `0.145.1+26.1`
- Java source/target/release: `25`
- Dependency configurations:
  - `modImplementation` -> `implementation`
  - no `mappings` dependency line
  - no `remapJar` usage

## Source namespace changes

Representative Yarn-to-Mojang name changes encountered during migration:

| Yarn name | Mojang official name |
| --- | --- |
| `net.minecraft.util.math.BlockPos` | `net.minecraft.core.BlockPos` |
| `net.minecraft.server.command.ServerCommandSource` | `net.minecraft.commands.CommandSourceStack` |
| `net.minecraft.server.network.ServerPlayerEntity` | `net.minecraft.server.level.ServerPlayer` |
| `net.minecraft.server.world.ServerWorld` | `net.minecraft.server.level.ServerLevel` |
| `net.minecraft.registry.Registries` | `net.minecraft.core.registries.BuiltInRegistries` / `Registries` |
| `net.minecraft.block.BlockState` | `net.minecraft.world.level.block.state.BlockState` |
| `net.minecraft.client.MinecraftClient` | `net.minecraft.client.Minecraft` |
| `net.minecraft.client.network.ClientPlayerEntity` | `net.minecraft.client.player.LocalPlayer` |
| `net.minecraft.text.Text` | `net.minecraft.network.chat.Component` |

## Client chat capture changes

`ChatMessageCaptureMixin` now targets `net.minecraft.client.gui.components.ChatComponent` and injects into the private `addMessage(Component, MessageSignature, GuiMessageSource, GuiMessageTag)` method. In Minecraft `26.1`, chat GUI helper classes moved under `net.minecraft.client.multiplayer.chat`.

## Validation performed

During the migration, these checks should pass before release:

```bash
./gradlew compileJava --console=plain
./gradlew compileClientJava --console=plain
./gradlew test --console=plain
./gradlew clean check --console=plain
```

Runtime smoke tests should also be performed when a graphical/client runtime is available:

```bash
./gradlew runServer --console=plain
./gradlew runClient --console=plain
```

Verify the MCP HTTP server starts and exercise at least:

- `/mcp/initialize`
- `/mcp/ping`
- `/mcp/tools/list`
- `/mcp/tools/call` with `execute_commands`
- `get_player_info`
- `get_blocks_in_area`
- client-only `take_screenshot`

## Follow-up for 26.2+

Fabric notes that `26.2` snapshots introduce renderer backend changes around OpenGL/Vulkan. This mod has limited rendering interaction, but screenshot utilities and any direct client rendering assumptions should be re-tested before updating beyond `26.1`.
