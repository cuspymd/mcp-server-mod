package cuspymd.mcp.mod.command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ChatMessageCaptureTest {
    private final ChatMessageCapture capture = ChatMessageCapture.getInstance();

    @BeforeEach
    public void resetCaptureState() {
        capture.stopCapturing();
        capture.startCapturing();
        capture.drainAvailableMessages();
        capture.stopCapturing();
    }

    @Test
    public void captureMessageIsIgnoredWhenNotCapturing() throws Exception {
        capture.captureMessage("ignored");

        assertNull(capture.waitForMessage(30));
    }

    @Test
    public void startCapturingEnablesQueueAndClearPreviousMessages() {
        capture.startCapturing();
        capture.captureMessage("first");
        assertEquals(1, capture.drainAvailableMessages().size());

        capture.startCapturing(); // should clear queue
        assertEquals(0, capture.drainAvailableMessages().size());
    }

    @Test
    public void waitForMessageWithFilterReturnsMatchingMessage() throws Exception {
        capture.startCapturing();
        capture.captureMessage("alpha");
        capture.captureMessage("beta");

        String result = capture.waitForMessage(150, m -> m.contains("bet"));

        assertEquals("beta", result);
    }

    @Test
    public void drainAvailableMessagesReturnsAllCapturedMessages() {
        capture.startCapturing();
        capture.captureMessage("one");
        capture.captureMessage("two");

        List<String> drained = capture.drainAvailableMessages();

        assertEquals(List.of("one", "two"), drained);
        assertEquals(0, capture.drainAvailableMessages().size());
    }
}
