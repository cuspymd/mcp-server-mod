package cuspymd.mcp.mod;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import cuspymd.mcp.mod.bridge.HTTPMCPServer;
import cuspymd.mcp.mod.config.MCPConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MCPServerModServer implements DedicatedServerModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("mcp-server-mod");
    private HTTPMCPServer httpServer;

    @Override
    public void onInitializeServer() {
        LOGGER.info("Initializing Minecraft MCP Dedicated Server");

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try {
                MCPConfig config = MCPConfig.load();
                if (config.getServer().isAutoStart()) {
                    String transport = config.getServer().getTransport();

                    if ("http".equals(transport)) {
                        httpServer = new HTTPMCPServer(config,
                            new cuspymd.mcp.mod.server.tools.ServerCommandExecutor(config, server),
                            new cuspymd.mcp.mod.server.tools.ServerPlayerInfoProvider(server),
                            new cuspymd.mcp.mod.server.tools.ServerBlockScanner(server),
                            new cuspymd.mcp.mod.server.tools.ServerScreenshotUtils(),
                            false
                        );
                        httpServer.start();
                        LOGGER.info("HTTP MCP Server started on port {}", httpServer.getPort());
                    } else {
                        LOGGER.warn("Unsupported transport: {}. Only 'http' is supported.", transport);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Failed to start MCP Server", e);
            }
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (httpServer != null) {
                httpServer.stop();
                LOGGER.info("HTTP MCP Server stopped");
            }
        });
    }
}
