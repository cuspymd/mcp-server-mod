package cuspymd.mcp.mod.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandParser {
    
    private static final Pattern QUOTED_ARGS_PATTERN = Pattern.compile("\"([^\"]*)\"|'([^']*)'|(\\S+)");
    private static final Pattern COORDINATE_PATTERN = Pattern.compile("(~?-?\\d+(?:\\.\\d+)?|\\^-?\\d+(?:\\.\\d+)?)");
    private static final Pattern SELECTOR_PATTERN = Pattern.compile("@[aeprs](?:\\[([^\\]]+)\\])?");
    
    public static class ParsedCommand {
        private final String baseCommand;
        private final List<String> arguments;
        
        public ParsedCommand(String baseCommand, List<String> arguments) {
            this.baseCommand = baseCommand;
            this.arguments = arguments;
        }
        
        public String getBaseCommand() { return baseCommand; }
        public List<String> getArguments() { return arguments; }
        public int getArgumentCount() { return arguments.size(); }
        
        public String getArgument(int index) {
            return index < arguments.size() ? arguments.get(index) : null;
        }
        
        public String getArgumentOrDefault(int index, String defaultValue) {
            String arg = getArgument(index);
            return arg != null ? arg : defaultValue;
        }
        
        public boolean hasMinimumArguments(int minArgs) {
            return arguments.size() >= minArgs;
        }
    }
    
    public static ParsedCommand parseCommand(String command) {
        if (command == null || command.trim().isEmpty()) {
            throw new IllegalArgumentException("Command cannot be null or empty");
        }
        
        String trimmedCommand = command.trim();
        if (trimmedCommand.startsWith("/")) {
            trimmedCommand = trimmedCommand.substring(1);
        }
        
        List<String> parts = parseArguments(trimmedCommand);
        if (parts.isEmpty()) {
            throw new IllegalArgumentException("Invalid command format");
        }
        
        String baseCommand = parts.get(0);
        List<String> arguments = parts.subList(1, parts.size());
        
        return new ParsedCommand(baseCommand, arguments);
    }
    
    public static List<String> parseArguments(String input) {
        List<String> arguments = new ArrayList<>();
        Matcher matcher = QUOTED_ARGS_PATTERN.matcher(input);
        
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                arguments.add(matcher.group(1));
            } else if (matcher.group(2) != null) {
                arguments.add(matcher.group(2));
            } else if (matcher.group(3) != null) {
                arguments.add(matcher.group(3));
            }
        }
        
        return arguments;
    }
    
    public static boolean isCoordinate(String arg) {
        return COORDINATE_PATTERN.matcher(arg).matches();
    }
    
    public static boolean isSelector(String arg) {
        return SELECTOR_PATTERN.matcher(arg).matches();
    }
    
    public static boolean isRelativeCoordinate(String arg) {
        return arg.startsWith("~");
    }
    
    public static boolean isLocalCoordinate(String arg) {
        return arg.startsWith("^");
    }
    
    public static List<String> extractCoordinates(String command) {
        List<String> coordinates = new ArrayList<>();
        ParsedCommand parsed = parseCommand(command);
        
        for (String arg : parsed.getArguments()) {
            if (isCoordinate(arg)) {
                coordinates.add(arg);
            }
        }
        
        return coordinates;
    }
    
    public static List<String> extractSelectors(String command) {
        List<String> selectors = new ArrayList<>();
        ParsedCommand parsed = parseCommand(command);
        
        for (String arg : parsed.getArguments()) {
            if (isSelector(arg)) {
                selectors.add(arg);
            }
        }
        
        return selectors;
    }
    
    public static String normalizeCommand(String command) {
        ParsedCommand parsed = parseCommand(command);
        StringBuilder normalized = new StringBuilder(parsed.getBaseCommand());
        
        for (String arg : parsed.getArguments()) {
            normalized.append(" ").append(arg);
        }
        
        return normalized.toString();
    }
    
    public static boolean isDestructiveCommand(String command) {
        ParsedCommand parsed = parseCommand(command);
        String baseCmd = parsed.getBaseCommand().toLowerCase();
        
        return switch (baseCmd) {
            case "kill", "clear", "setblock" -> {
                List<String> selectors = extractSelectors(command);
                yield selectors.contains("@a") || selectors.contains("@e");
            }
            case "fill" -> {
                yield parsed.hasMinimumArguments(7) && 
                       "air".equals(parsed.getArgumentOrDefault(6, "").toLowerCase());
            }
            default -> false;
        };
    }
}