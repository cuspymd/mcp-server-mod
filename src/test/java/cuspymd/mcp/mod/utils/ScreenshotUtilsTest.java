package cuspymd.mcp.mod.utils;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class ScreenshotUtilsTest {

    @BeforeEach
    public void setUp() {
        ScreenshotUtils.pendingDeferredTasks.clear();
    }

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

    @Test
    public void testDeferredTaskLogic() {
        AtomicBoolean taskRun = new AtomicBoolean(false);
        ScreenshotUtils.pendingDeferredTasks.add(new ScreenshotUtils.DeferredTask(() -> taskRun.set(true), 2));

        // First tick
        ScreenshotUtils.onEndTick(null);
        assertFalse(taskRun.get(), "Task should not run after only 1 tick");
        assertEquals(1, ScreenshotUtils.pendingDeferredTasks.size());

        // Second tick
        ScreenshotUtils.onEndTick(null);
        assertTrue(taskRun.get(), "Task should run after 2 ticks");
        assertTrue(ScreenshotUtils.pendingDeferredTasks.isEmpty(), "Pending tasks should be empty after execution");
    }

    @Test
    public void testMultipleDeferredTasks() {
        AtomicBoolean task1Run = new AtomicBoolean(false);
        AtomicBoolean task2Run = new AtomicBoolean(false);

        ScreenshotUtils.pendingDeferredTasks.add(new ScreenshotUtils.DeferredTask(() -> task1Run.set(true), 1));
        ScreenshotUtils.pendingDeferredTasks.add(new ScreenshotUtils.DeferredTask(() -> task2Run.set(true), 3));

        // Tick 1
        ScreenshotUtils.onEndTick(null);
        assertTrue(task1Run.get());
        assertFalse(task2Run.get());
        assertEquals(1, ScreenshotUtils.pendingDeferredTasks.size());

        // Tick 2
        ScreenshotUtils.onEndTick(null);
        assertFalse(task2Run.get());

        // Tick 3
        ScreenshotUtils.onEndTick(null);
        assertTrue(task2Run.get());
        assertTrue(ScreenshotUtils.pendingDeferredTasks.isEmpty());
    }

    @Test
    public void testEncodeBytesToBase64() {
        byte[] data = "Hello, MCP!".getBytes();
        String expected = "SGVsbG8sIE1DUCE=";
        assertEquals(expected, ScreenshotUtils.encodeBytesToBase64(data));
    }

    @Test
    public void testBuildTeleportCommand_FormatsExpectedCommand() {
        String command = ScreenshotUtils.buildTeleportCommand(120.5, 70, -200.5, 180f, 0f);
        assertEquals("tp @s 120.500 70.000 -200.500 180.000 0.000", command);
    }

    @Test
    public void testBuildTeleportCommand_UsesRootLocaleDecimalPoint() {
        String command = ScreenshotUtils.buildTeleportCommand(1.25, 2.5, 3.75, 90f, -30f);
        assertTrue(command.contains("1.250 2.500 3.750 90.000 -30.000"));
    }

    @Test
    public void testBuildRotateCommand_FormatsExpectedCommand() {
        String command = ScreenshotUtils.buildRotateCommand(135f, -20f);
        assertEquals("tp @s ~ ~ ~ 135.000 -20.000", command);
    }

    @Test
    public void testSmallestAngleDifference_WrapAround() {
        assertEquals(20f, ScreenshotUtils.smallestAngleDifference(350f, 10f), 0.0001f);
        assertEquals(20f, ScreenshotUtils.smallestAngleDifference(10f, 350f), 0.0001f);
        assertEquals(0f, ScreenshotUtils.smallestAngleDifference(180f, -180f), 0.0001f);
    }
}
