package cuspymd.mcp.mod.server.tools;

import com.google.gson.JsonObject;
import cuspymd.mcp.mod.utils.IScreenshotUtils;
import cuspymd.mcp.mod.server.MCPProtocol;

import java.util.concurrent.CompletableFuture;

public class ServerScreenshotUtils implements IScreenshotUtils {
    @Override
    public CompletableFuture<String> takeScreenshot(JsonObject params) {
        CompletableFuture<String> future = new CompletableFuture<>();
        future.completeExceptionally(new UnsupportedOperationException("take_screenshot is not supported in dedicated server mode."));
        return future;
    }
}