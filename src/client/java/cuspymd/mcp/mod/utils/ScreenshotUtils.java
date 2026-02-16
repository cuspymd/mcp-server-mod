package cuspymd.mcp.mod.utils;

import com.google.gson.JsonObject;
import cuspymd.mcp.mod.config.MCPConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ScreenshotUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScreenshotUtils.class);
    private static final List<DeferredTask> pendingDeferredTasks = Collections.synchronizedList(new ArrayList<>());

    private static class DeferredTask {
        Runnable runnable;
        int remainingTicks;

        DeferredTask(Runnable runnable, int ticks) {
            this.runnable = runnable;
            this.remainingTicks = ticks;
        }
    }

    /**
     * Takes a screenshot of the current Minecraft game screen.
     * Optionally moves the player to a specified position and rotation before capturing.
     * This method must be called from the render thread or it will handle the thread switch itself.
     *
     * @param params JsonObject containing optional x, y, z, yaw, pitch
     * @return A CompletableFuture that completes with the Base64 encoded PNG image data
     */
    public static CompletableFuture<String> takeScreenshot(JsonObject params) {
        MinecraftClient client = MinecraftClient.getInstance();
        CompletableFuture<String> future = new CompletableFuture<>();

        // Ensure we are on the render thread for player manipulation and screenshotting
        if (!client.isOnThread()) {
            client.execute(() -> takeScreenshotInternal(client, params, future));
        } else {
            takeScreenshotInternal(client, params, future);
        }

        return future;
    }

    private static void takeScreenshotInternal(MinecraftClient client, JsonObject params, CompletableFuture<String> future) {
        try {
            if (client.player == null) {
                future.completeExceptionally(new Exception("Player not found. Make sure you are in a world."));
                return;
            }

            // Validate coordinates
            String validationError = validateCoordinates(params);
            if (validationError != null) {
                future.completeExceptionally(new Exception(validationError));
                return;
            }

            // Handle teleportation if coordinates are provided
            boolean moved = false;
            boolean hasX = params.has("x");
            boolean hasY = params.has("y");
            boolean hasZ = params.has("z");

            if (hasX && hasY && hasZ) {
                double x = params.get("x").getAsDouble();
                double y = params.get("y").getAsDouble();
                double z = params.get("z").getAsDouble();

                // Use current values as defaults if rotation not provided
                float yaw = params.has("yaw") ? params.get("yaw").getAsFloat() : client.player.getYaw();
                float pitch = params.has("pitch") ? params.get("pitch").getAsFloat() : client.player.getPitch();

                // Teleport the player
                client.player.refreshPositionAndAngles(x, y, z, yaw, pitch);
                LOGGER.info("Teleported player to {} {} {} (yaw: {}, pitch: {}) for screenshot", x, y, z, yaw, pitch);
                moved = true;
            } else {
                // Handle only rotation if coordinates are not provided but rotation is
                if (params.has("yaw")) {
                    client.player.setYaw(params.get("yaw").getAsFloat());
                    moved = true;
                }
                if (params.has("pitch")) {
                    client.player.setPitch(params.get("pitch").getAsFloat());
                    moved = true;
                }
            }

            // If we moved the player or changed their view, we MUST wait for the next frame
            // so that the render loop can update the framebuffer with the new view.
            if (moved) {
                // Defer capture to ensure the world is rendered with the new view.
                // We wait for 2 end-of-tick events to be certain a render has completed.
                pendingDeferredTasks.add(new DeferredTask(() -> captureNow(client, future), 2));
            } else {
                // Take screenshot immediately if no movement occurred
                captureNow(client, future);
            }
        } catch (Exception e) {
            LOGGER.error("Unexpected error taking screenshot", e);
            future.completeExceptionally(e);
        }
    }

    /**
     * Validates that if any coordinate is provided, all three (x, y, z) are provided.
     * @return null if valid, or an error message if invalid.
     */
    static String validateCoordinates(JsonObject params) {
        boolean hasX = params.has("x");
        boolean hasY = params.has("y");
        boolean hasZ = params.has("z");

        if ((hasX || hasY || hasZ) && !(hasX && hasY && hasZ)) {
            return "Partial coordinates provided. You must provide all three: x, y, and z.";
        }
        return null;
    }

    /**
     * Called by ClientTickEvents.END_CLIENT_TICK to process deferred screenshot tasks.
     */
    public static void onEndTick(MinecraftClient client) {
        synchronized (pendingDeferredTasks) {
            Iterator<DeferredTask> it = pendingDeferredTasks.iterator();
            while (it.hasNext()) {
                DeferredTask task = it.next();
                task.remainingTicks--;
                if (task.remainingTicks <= 0) {
                    try {
                        task.runnable.run();
                    } catch (Exception e) {
                        LOGGER.error("Error executing deferred screenshot task", e);
                    }
                    it.remove();
                }
            }
        }
    }

    private static void saveDebugScreenshot(Path tempFile) {
        try {
            Path gameDir = FabricLoader.getInstance().getGameDir();
            Path debugDir = gameDir.resolve("mcp_debug_screenshots");
            if (!Files.exists(debugDir)) {
                Files.createDirectories(debugDir);
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
            Path targetFile = debugDir.resolve("screenshot_" + timestamp + ".png");

            Files.copy(tempFile, targetFile);
            LOGGER.info("Saved debug screenshot to: {}", targetFile.toAbsolutePath());
        } catch (IOException e) {
            LOGGER.error("Failed to save debug screenshot", e);
        }
    }

    /**
     * Captures the current framebuffer and completes the future with the Base64 data.
     */
    private static void captureNow(MinecraftClient client, CompletableFuture<String> future) {
        try {
            Framebuffer framebuffer = client.getFramebuffer();

            ScreenshotRecorder.takeScreenshot(framebuffer, (nativeImage) -> {
                Path tempFile = null;
                boolean shouldSaveDebug = false;
                try {
                    MCPConfig config = MCPConfig.load();
                    shouldSaveDebug = config.getClient().isSaveScreenshotsForDebug();

                    // Create a temporary file to save the PNG
                    tempFile = Files.createTempFile("mcp_screenshot", ".png");
                    nativeImage.writeTo(tempFile);

                    // Read the file bytes and encode to Base64
                    byte[] bytes = Files.readAllBytes(tempFile);
                    String base64 = Base64.getEncoder().encodeToString(bytes);

                    // If debug mode is on, save a permanent copy
                    if (shouldSaveDebug) {
                        saveDebugScreenshot(tempFile);
                    }

                    future.complete(base64);
                } catch (IOException e) {
                    LOGGER.error("IO error while processing screenshot", e);
                    future.completeExceptionally(e);
                } finally {
                    nativeImage.close();
                    if (tempFile != null) {
                        try {
                            Files.deleteIfExists(tempFile);
                        } catch (IOException ignored) {}
                    }
                }
            });
        } catch (Exception e) {
            LOGGER.error("Error during capture", e);
            future.completeExceptionally(e);
        }
    }
}
