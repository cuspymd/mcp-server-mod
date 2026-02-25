package cuspymd.mcp.mod.command;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class CommandOutcomeAnalyzer {
    private static final List<String> FAILURE_MARKERS = List.of(
        "cannot",
        "failed",
        "unknown",
        "no entity was found",
        "is not holding any item",
        "invalid",
        "error"
    );

    private static final List<String> SUCCESS_MARKERS = List.of(
        "successfully",
        "teleported",
        "summoned",
        "given",
        "gave",
        "set the weather",
        "set the time",
        "filled",
        "set block",
        "changed the block"
    );

    private CommandOutcomeAnalyzer() {
    }

    static boolean hasFailureMarker(String message) {
        return containsAnyMarker(message, FAILURE_MARKERS);
    }

    static boolean hasSuccessMarker(String message) {
        return containsAnyMarker(message, SUCCESS_MARKERS);
    }

    static boolean hasKnownOutcomeMarker(String message) {
        return hasFailureMarker(message) || hasSuccessMarker(message);
    }

    public static Outcome analyze(boolean accepted, List<String> chatMessages, String fallbackSummary) {
        if (!accepted) {
            String summary = fallbackSummary != null && !fallbackSummary.isBlank()
                ? fallbackSummary
                : "Command was not accepted for execution";
            return new Outcome(false, false, "execution_error", summary);
        }

        Optional<String> firstFailure = firstMatching(chatMessages, FAILURE_MARKERS);
        if (firstFailure.isPresent()) {
            return new Outcome(true, false, "rejected_by_game", firstFailure.get());
        }

        Optional<String> firstSuccess = firstMatching(chatMessages, SUCCESS_MARKERS);
        if (firstSuccess.isPresent()) {
            return new Outcome(true, true, "applied", firstSuccess.get());
        }

        if (chatMessages != null && !chatMessages.isEmpty()) {
            return new Outcome(true, null, "unknown", chatMessages.get(0));
        }

        return new Outcome(true, null, "unknown", "No command feedback captured");
    }

    private static Optional<String> firstMatching(List<String> messages, List<String> markers) {
        if (messages == null || messages.isEmpty()) {
            return Optional.empty();
        }

        for (String message : messages) {
            if (containsAnyMarker(message, markers)) {
                return Optional.of(message);
            }
        }
        return Optional.empty();
    }

    private static boolean containsAnyMarker(String message, List<String> markers) {
        if (message == null || markers == null || markers.isEmpty()) {
            return false;
        }

        String normalized = message.toLowerCase(Locale.ROOT);
        for (String marker : markers) {
            if (normalized.contains(marker)) {
                return true;
            }
        }
        return false;
    }

    public record Outcome(boolean accepted, Boolean applied, String status, String summary) {
    }
}
