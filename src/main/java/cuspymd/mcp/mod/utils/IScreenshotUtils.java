package cuspymd.mcp.mod.utils;

import com.google.gson.JsonObject;
import java.util.concurrent.CompletableFuture;

public interface IScreenshotUtils {
    CompletableFuture<String> takeScreenshot(JsonObject params);
}
