package cuspymd.mcp.mod.utils;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ScreenshotUtilsTest {

    @Test
    public void testValidateCoordinates_AllProvided() {
        JsonObject params = new JsonObject();
        params.addProperty("x", 100);
        params.addProperty("y", 64);
        params.addProperty("z", 100);

        assertNull(ScreenshotUtils.validateCoordinates(params));
    }

    @Test
    public void testValidateCoordinates_NoneProvided() {
        JsonObject params = new JsonObject();
        // No x, y, or z
        params.addProperty("yaw", 90);

        assertNull(ScreenshotUtils.validateCoordinates(params));
    }

    @Test
    public void testValidateCoordinates_PartialX() {
        JsonObject params = new JsonObject();
        params.addProperty("x", 100);

        String error = ScreenshotUtils.validateCoordinates(params);
        assertNotNull(error);
        assertTrue(error.contains("Partial coordinates provided"));
    }

    @Test
    public void testValidateCoordinates_PartialXY() {
        JsonObject params = new JsonObject();
        params.addProperty("x", 100);
        params.addProperty("y", 64);

        String error = ScreenshotUtils.validateCoordinates(params);
        assertNotNull(error);
        assertTrue(error.contains("Partial coordinates provided"));
    }

    @Test
    public void testValidateCoordinates_PartialXZ() {
        JsonObject params = new JsonObject();
        params.addProperty("x", 100);
        params.addProperty("z", 100);

        String error = ScreenshotUtils.validateCoordinates(params);
        assertNotNull(error);
        assertTrue(error.contains("Partial coordinates provided"));
    }
}
