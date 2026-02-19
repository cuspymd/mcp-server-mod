package cuspymd.mcp.mod.bridge;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import cuspymd.mcp.mod.config.MCPConfig;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

public class HTTPMCPServerTest {

    @Test
    public void testAwaitScreenshotResult_Success() {
        HTTPMCPServer server = new HTTPMCPServer(new MCPConfig());
        CompletableFuture<String> future = CompletableFuture.completedFuture("abc123");

        JsonObject response = server.awaitScreenshotResult(future);

        assertFalse(response.get("isError").getAsBoolean());
        JsonObject firstContent = response.getAsJsonArray("content").get(0).getAsJsonObject();
        assertEquals("image", firstContent.get("type").getAsString());
        assertEquals("abc123", firstContent.get("data").getAsString());
        assertEquals("image/png", firstContent.get("mimeType").getAsString());
    }

    @Test
    public void testAwaitScreenshotResult_Timeout() {
        HTTPMCPServer server = new HTTPMCPServer(new MCPConfig());
        TimeoutFuture future = new TimeoutFuture();

        JsonObject response = server.awaitScreenshotResult(future);

        assertTrue(response.get("isError").getAsBoolean());
        assertTrue(extractText(response).contains("timed out"));
        assertTrue(future.cancelCalled, "Future should be cancelled on timeout");
    }

    @Test
    public void testAwaitScreenshotResult_Interrupted() {
        HTTPMCPServer server = new HTTPMCPServer(new MCPConfig());
        InterruptedFuture future = new InterruptedFuture();

        try {
            JsonObject response = server.awaitScreenshotResult(future);
            assertTrue(response.get("isError").getAsBoolean());
            assertTrue(extractText(response).contains("interrupted"));
            assertTrue(future.cancelCalled, "Future should be cancelled on interruption");
            assertTrue(Thread.currentThread().isInterrupted(), "Interrupted flag should be preserved");
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    public void testAwaitScreenshotResult_ExecutionFailure() {
        HTTPMCPServer server = new HTTPMCPServer(new MCPConfig());
        CompletableFuture<String> future = new CompletableFuture<>();
        future.completeExceptionally(new IllegalStateException("capture failed"));

        JsonObject response = server.awaitScreenshotResult(future);

        assertTrue(response.get("isError").getAsBoolean());
        assertTrue(extractText(response).contains("capture failed"));
    }

    @Test
    public void testHandleMCPRequest_ToolsCallTakeScreenshotSuccess() throws Exception {
        TestableHTTPMCPServer server = new TestableHTTPMCPServer(new MCPConfig());
        server.nextFuture = CompletableFuture.completedFuture("img-data");

        JsonObject request = new JsonObject();
        request.addProperty("jsonrpc", "2.0");
        request.addProperty("id", 7);
        request.addProperty("method", "tools/call");
        JsonObject params = new JsonObject();
        params.addProperty("name", "take_screenshot");
        params.add("arguments", new JsonObject());
        request.add("params", params);

        JsonObject response = invokeHandleMCPRequest(server, request);

        assertEquals("2.0", response.get("jsonrpc").getAsString());
        assertEquals(7, response.get("id").getAsInt());
        JsonObject result = response.getAsJsonObject("result");
        assertFalse(result.get("isError").getAsBoolean());
        JsonObject firstContent = result.getAsJsonArray("content").get(0).getAsJsonObject();
        assertEquals("image", firstContent.get("type").getAsString());
        assertEquals("img-data", firstContent.get("data").getAsString());
    }

    @Test
    public void testHandleMCPRequest_ToolsCallTakeScreenshotTimeout() throws Exception {
        TestableHTTPMCPServer server = new TestableHTTPMCPServer(new MCPConfig());
        server.nextFuture = new TimeoutFuture();

        JsonObject request = new JsonObject();
        request.addProperty("jsonrpc", "2.0");
        request.addProperty("id", 8);
        request.addProperty("method", "tools/call");
        JsonObject params = new JsonObject();
        params.addProperty("name", "take_screenshot");
        params.add("arguments", new JsonObject());
        request.add("params", params);

        JsonObject response = invokeHandleMCPRequest(server, request);

        JsonObject result = response.getAsJsonObject("result");
        assertTrue(result.get("isError").getAsBoolean());
        assertTrue(extractText(result).contains("timed out"));
    }

    @Test
    public void testHandleMCPRequest_ToolsCallTakeScreenshotWithoutArguments() throws Exception {
        TestableHTTPMCPServer server = new TestableHTTPMCPServer(new MCPConfig());
        server.nextFuture = CompletableFuture.completedFuture("no-args-image");

        JsonObject request = new JsonObject();
        request.addProperty("jsonrpc", "2.0");
        request.addProperty("id", 9);
        request.addProperty("method", "tools/call");
        JsonObject params = new JsonObject();
        params.addProperty("name", "take_screenshot");
        request.add("params", params);

        JsonObject response = invokeHandleMCPRequest(server, request);

        JsonObject result = response.getAsJsonObject("result");
        assertFalse(result.get("isError").getAsBoolean());
        JsonObject firstContent = result.getAsJsonArray("content").get(0).getAsJsonObject();
        assertEquals("no-args-image", firstContent.get("data").getAsString());
    }

    private JsonObject invokeHandleMCPRequest(HTTPMCPServer server, JsonObject request) throws Exception {
        Method method = HTTPMCPServer.class.getDeclaredMethod("handleMCPRequest", JsonObject.class);
        method.setAccessible(true);
        return (JsonObject) method.invoke(server, request);
    }

    private String extractText(JsonObject response) {
        JsonArray content = response.getAsJsonArray("content");
        return content.get(0).getAsJsonObject().get("text").getAsString();
    }

    private static class TimeoutFuture extends CompletableFuture<String> {
        boolean cancelCalled = false;

        @Override
        public String get(long timeout, TimeUnit unit) throws TimeoutException {
            throw new TimeoutException("forced timeout");
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            cancelCalled = true;
            return super.cancel(mayInterruptIfRunning);
        }
    }

    private static class InterruptedFuture extends CompletableFuture<String> {
        boolean cancelCalled = false;

        @Override
        public String get(long timeout, TimeUnit unit) throws InterruptedException {
            throw new InterruptedException("forced interrupt");
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            cancelCalled = true;
            return super.cancel(mayInterruptIfRunning);
        }
    }

    private static class TestableHTTPMCPServer extends HTTPMCPServer {
        CompletableFuture<String> nextFuture;

        TestableHTTPMCPServer(MCPConfig config) {
            super(config);
        }

        @Override
        CompletableFuture<String> takeScreenshotAsync(JsonObject params) {
            return nextFuture;
        }
    }
}
