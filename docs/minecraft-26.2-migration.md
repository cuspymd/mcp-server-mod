# Minecraft 26.2 Migration Notes

## Summary

This branch ports `mcp-server-mod` from Minecraft `26.1` to Minecraft `26.2`, both on Mojang official/unobfuscated names. No Yarn/mapping changes were involved; this is a point-release bump.

## Key build changes

- Minecraft: `26.2`
- Fabric API: `0.154.1+26.2`
- Fabric Loader: `0.19.3` (unchanged)
- Loom: `1.17` (unchanged)
- `fabric.mod.json` `depends.minecraft` range: `~26.1` -> `~26.2`

## Source changes

Minecraft `26.2` introduces the initial Vulkan renderer backend alongside OpenGL. As part of this, `Minecraft.getMainRenderTarget()` was removed; the main render target now lives on `GameRenderer`:

| 26.1 | 26.2 |
| --- | --- |
| `client.getMainRenderTarget()` | `client.gameRenderer.mainRenderTarget()` |

`Screenshot.takeScreenshot(RenderTarget, Consumer<NativeImage>)` is unchanged, so `ScreenshotUtils` required only the accessor fix (`src/client/java/cuspymd/mcp/mod/utils/ScreenshotUtils.java`).

## Validation performed

```bash
./gradlew compileJava --console=plain
./gradlew compileClientJava --console=plain
./gradlew test --console=plain
./gradlew clean check --console=plain
./gradlew runServer --console=plain
```

`runServer` confirmed the mod loads and `MCP Server Mod initialized` / dedicated-server MCP init logs appear before the server halts on the (expected, dev-environment-only) EULA prompt.

Runtime client smoke testing (`runClient`) and the screenshot tool still need a graphical client environment, ideally run once with the Vulkan renderer enabled to confirm `take_screenshot` behaves the same as under OpenGL.

## Follow-up

- Re-test `take_screenshot` under both the OpenGL and Vulkan render backends once a graphical environment is available.
