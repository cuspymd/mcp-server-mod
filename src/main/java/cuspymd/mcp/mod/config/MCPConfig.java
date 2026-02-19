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
    
    private ServerConfig server = new ServerConfig();
    private ClientConfig client = new ClientConfig();
    private SafetyConfig safety = new SafetyConfig();
    
    public static MCPConfig load() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        Path configFile = configDir.resolve("mcp-client.json");
        
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
        Path configFile = configDir.resolve("mcp-client.json");
        
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
        private int maxAreaSize = 48;
        private List<String> allowedCommands = List.of("fill", "clone", "setblock", "summon", "tp", "give", "gamemode", "effect", "enchant", "weather", "time", "say", "tell", "title");
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
        
        public boolean isShowNotifications() { return showNotifications; }
        public String getLogLevel() { return logLevel; }
        public boolean isLogCommands() { return logCommands; }
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