package cuspymd.mcp.mod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class MCPConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(MCPConfig.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    public static final List<String> DEFAULT_ALLOWED_COMMANDS = List.of(
        "fill", "clone", "setblock", "summon", "tp", "give", "gamemode",
        "effect", "enchant", "weather", "time", "say", "tell", "title"
    );

    private ServerConfig server = new ServerConfig();
    private ClientConfig client = new ClientConfig();
    private SafetyConfig safety = new SafetyConfig();
    
    public static MCPConfig load() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        String configFileName = "mcp.json";
        Path configFile = configDir.resolve(configFileName);

        // Backwards compatibility with mcp-client.json
        Path oldConfigFile = configDir.resolve("mcp-client.json");
        if (Files.exists(oldConfigFile) && !Files.exists(configFile)) {
            try {
                Files.move(oldConfigFile, configFile);
            } catch (IOException e) {
                LOGGER.warn("Failed to rename mcp-client.json to mcp.json", e);
                configFile = oldConfigFile;
            }
        }
        
        if (Files.exists(configFile)) {
            try {
                String json = Files.readString(configFile);
                return GSON.fromJson(json, MCPConfig.class);
            } catch (IOException e) {
                LOGGER.warn("Failed to load config file, using defaults", e);
            }
        }
        
        MCPConfig defaultConfig = new MCPConfig();
        defaultConfig.save();
        return defaultConfig;
    }
    
    public void save() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        Path configFile = configDir.resolve("mcp.json");
        
        try {
            Files.createDirectories(configDir);
            String json = GSON.toJson(this);
            Files.writeString(configFile, json);
        } catch (IOException e) {
            LOGGER.error("Failed to save config file", e);
        }
    }
    
    public ServerConfig getServer() { return server; }
    public ClientConfig getClient() { return client; }
    public SafetyConfig getSafety() { return safety; }
    
    public static class ServerConfig {
        private String transport = "http"; // "stdio" or "http"
        private int port = 8080;
        private String host = "localhost";
        private boolean enableSafety = true;
        private int maxAreaSize = 10;
        private List<String> allowedCommands = DEFAULT_ALLOWED_COMMANDS;
        private int requestTimeoutMs = 30000;
        private boolean autoStart = true;
        
        public String getTransport() { return transport; }
        public int getPort() { return port; }
        public String getHost() { return host; }
        public boolean isEnableSafety() { return enableSafety; }
        public int getMaxAreaSize() { return maxAreaSize; }
        public List<String> getAllowedCommands() { return allowedCommands; }
        public int getRequestTimeoutMs() { return requestTimeoutMs; }
        public boolean isAutoStart() { return autoStart; }
    }
    
    public static class ClientConfig {
        private boolean showNotifications = true;
        private String logLevel = "INFO";
        private boolean logCommands = false;
        private boolean saveScreenshotsForDebug = false;
        
        public boolean isShowNotifications() { return showNotifications; }
        public String getLogLevel() { return logLevel; }
        public boolean isLogCommands() { return logCommands; }
        public boolean isSaveScreenshotsForDebug() { return saveScreenshotsForDebug; }
    }
    
    public static class SafetyConfig {
        private int maxEntitiesPerCommand = 10;
        private int maxBlocksPerCommand = 125000;
        private boolean blockCreativeForAll = true;
        private boolean requireOpForAdminCommands = true;
        
        public int getMaxEntitiesPerCommand() { return maxEntitiesPerCommand; }
        public int getMaxBlocksPerCommand() { return maxBlocksPerCommand; }
        public boolean isBlockCreativeForAll() { return blockCreativeForAll; }
        public boolean isRequireOpForAdminCommands() { return requireOpForAdminCommands; }
    }
}