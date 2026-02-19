package cuspymd.mcp.mod.safety;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

public final class CommandSafetyPolicy {
    private static final List<String> HARD_DENY_COMMANDS = List.of(
        "op",
        "deop",
        "stop",
        "reload",
        "ban",
        "pardon",
        "ban-ip",
        "pardon-ip",
        "whitelist",
        "save-all",
        "save-off",
        "save-on",
        "debug"
    );

    private CommandSafetyPolicy() {
    }

    public static boolean isHardDenied(String commandName) {
        String normalized = normalizeCommandName(commandName);
        return HARD_DENY_COMMANDS.contains(normalized);
    }

    public static List<String> filterAllowedCommands(List<String> configuredAllowedCommands) {
        if (configuredAllowedCommands == null || configuredAllowedCommands.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<String> filtered = new LinkedHashSet<>();
        for (String rawCommand : configuredAllowedCommands) {
            String normalized = normalizeCommandName(rawCommand);
            if (!normalized.isEmpty() && !isHardDenied(normalized)) {
                filtered.add(normalized);
            }
        }
        return List.copyOf(filtered);
    }

    private static String normalizeCommandName(String raw) {
        if (raw == null) {
            return "";
        }

        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        String[] parts = normalized.split("\\s+");
        return parts.length == 0 ? "" : parts[0];
    }
}
