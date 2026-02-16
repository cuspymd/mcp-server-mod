package cuspymd.mcp.mod;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import cuspymd.mcp.mod.server.MCPProtocol;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MCPProtocolTest {

    @Test
    public void testCreateImageResponse() {
        String base64Data = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==";
        String mimeType = "image/png";

        JsonObject response = MCPProtocol.createImageResponse(base64Data, mimeType);

        assertFalse(response.get("isError").getAsBoolean());
        assertTrue(response.has("content"));

        JsonArray content = response.getAsJsonArray("content");
        assertEquals(1, content.size());

        JsonObject imageContent = content.get(0).getAsJsonObject();
        assertEquals("image", imageContent.get("type").getAsString());
        assertEquals(base64Data, imageContent.get("data").getAsString());
        assertEquals(mimeType, imageContent.get("mimeType").getAsString());
    }
}
