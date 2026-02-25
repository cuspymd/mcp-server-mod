package cuspymd.mcp.mod.command;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CommandExecutorMessageFilterTest {

    @Test
    public void playerChatNoiseIsExcludedFromOutcomeCandidates() {
        List<ChatMessageCapture.CapturedMessage> messages = List.of(
            new ChatMessageCapture.CapturedMessage(
                "we successfully finished our base",
                100L,
                ChatMessageCapture.MessageSource.PLAYER_CHAT
            ),
            new ChatMessageCapture.CapturedMessage(
                "Successfully filled 4 block(s)",
                120L,
                ChatMessageCapture.MessageSource.SYSTEM
            )
        );

        List<String> filtered = CommandExecutor.selectMessagesForOutcome("fill 0 0 0 1 1 1 stone", messages);

        assertEquals(List.of("Successfully filled 4 block(s)"), filtered);
    }

    @Test
    public void mismatchedSuccessMarkerIsDropped() {
        List<ChatMessageCapture.CapturedMessage> messages = List.of(
            new ChatMessageCapture.CapturedMessage(
                "Set the weather to clear",
                100L,
                ChatMessageCapture.MessageSource.SYSTEM
            )
        );

        List<String> filtered = CommandExecutor.selectMessagesForOutcome("time set 1000", messages);

        assertEquals(List.of(), filtered);
    }

    @Test
    public void nonPlayerMessagesAreUsedAsFallbackWhenNoKnownMarkersExist() {
        List<ChatMessageCapture.CapturedMessage> messages = List.of(
            new ChatMessageCapture.CapturedMessage(
                "Server notice: area protected",
                100L,
                ChatMessageCapture.MessageSource.SYSTEM
            ),
            new ChatMessageCapture.CapturedMessage(
                "<Player> cannot believe this",
                120L,
                ChatMessageCapture.MessageSource.PLAYER_CHAT
            )
        );

        List<String> filtered = CommandExecutor.selectMessagesForOutcome("clone 0 0 0 1 1 1 2 2 2", messages);

        assertEquals(List.of("Server notice: area protected"), filtered);
    }

    @Test
    public void chatOutputCommandDoesNotTreatMessageBodyAsOutcomeSignal() {
        List<ChatMessageCapture.CapturedMessage> messages = List.of(
            new ChatMessageCapture.CapturedMessage(
                "[Player285] successfully cannot",
                100L,
                ChatMessageCapture.MessageSource.SYSTEM
            )
        );

        List<String> filtered = CommandExecutor.selectMessagesForOutcome("say successfully cannot", messages);

        assertEquals(List.of(), filtered);
    }

    @Test
    public void chatOutputCommandStillKeepsExplicitParseError() {
        List<ChatMessageCapture.CapturedMessage> messages = List.of(
            new ChatMessageCapture.CapturedMessage(
                "Expected whitespace to end one argument, but found trailing data",
                100L,
                ChatMessageCapture.MessageSource.SYSTEM
            ),
            new ChatMessageCapture.CapturedMessage(
                "say test extra<--[HERE]",
                120L,
                ChatMessageCapture.MessageSource.SYSTEM
            )
        );

        List<String> filtered = CommandExecutor.selectMessagesForOutcome("say test", messages);

        assertEquals(
            List.of(
                "Expected whitespace to end one argument, but found trailing data",
                "say test extra<--[HERE]"
            ),
            filtered
        );
    }
}
