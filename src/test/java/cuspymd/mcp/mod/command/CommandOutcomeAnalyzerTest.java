package cuspymd.mcp.mod.command;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CommandOutcomeAnalyzerTest {

    @Test
    public void successMessageIsClassifiedAsApplied() {
        CommandOutcomeAnalyzer.Outcome outcome = CommandOutcomeAnalyzer.analyze(
            true,
            List.of("Successfully filled 4 block(s)"),
            "fallback"
        );

        assertTrue(outcome.accepted());
        assertTrue(outcome.applied());
        assertEquals("applied", outcome.status());
        assertEquals("Successfully filled 4 block(s)", outcome.summary());
    }

    @Test
    public void failureMessageIsClassifiedAsRejectedByGame() {
        CommandOutcomeAnalyzer.Outcome outcome = CommandOutcomeAnalyzer.analyze(
            true,
            List.of("Carrot cannot support that enchantment"),
            "fallback"
        );

        assertTrue(outcome.accepted());
        assertFalse(outcome.applied());
        assertEquals("rejected_by_game", outcome.status());
    }

    @Test
    public void noMessageIsClassifiedAsUnknown() {
        CommandOutcomeAnalyzer.Outcome outcome = CommandOutcomeAnalyzer.analyze(
            true,
            List.of(),
            "fallback"
        );

        assertTrue(outcome.accepted());
        assertNull(outcome.applied());
        assertEquals("unknown", outcome.status());
    }

    @Test
    public void holdingItemFailureIsClassifiedAsRejectedByGame() {
        CommandOutcomeAnalyzer.Outcome outcome = CommandOutcomeAnalyzer.analyze(
            true,
            List.of("Player691 is not holding any item"),
            "fallback"
        );

        assertTrue(outcome.accepted());
        assertFalse(outcome.applied());
        assertEquals("rejected_by_game", outcome.status());
        assertEquals("Player691 is not holding any item", outcome.summary());
    }

    @Test
    public void gaveMessageIsClassifiedAsApplied() {
        CommandOutcomeAnalyzer.Outcome outcome = CommandOutcomeAnalyzer.analyze(
            true,
            List.of("Gave 1 [Dirt] to Player691"),
            "fallback"
        );

        assertTrue(outcome.accepted());
        assertTrue(outcome.applied());
        assertEquals("applied", outcome.status());
        assertEquals("Gave 1 [Dirt] to Player691", outcome.summary());
    }

    @Test
    public void changedBlockMessageIsClassifiedAsApplied() {
        CommandOutcomeAnalyzer.Outcome outcome = CommandOutcomeAnalyzer.analyze(
            true,
            List.of("Changed the block at -24, 72, -29"),
            "fallback"
        );

        assertTrue(outcome.accepted());
        assertTrue(outcome.applied());
        assertEquals("applied", outcome.status());
        assertEquals("Changed the block at -24, 72, -29", outcome.summary());
    }

    @Test
    public void setTimeMessageIsClassifiedAsApplied() {
        CommandOutcomeAnalyzer.Outcome outcome = CommandOutcomeAnalyzer.analyze(
            true,
            List.of("Set the time to 1000"),
            "fallback"
        );

        assertTrue(outcome.accepted());
        assertTrue(outcome.applied());
        assertEquals("applied", outcome.status());
        assertEquals("Set the time to 1000", outcome.summary());
    }
}
