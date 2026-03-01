package cuspymd.mcp.mod;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import cuspymd.mcp.mod.bridge.HTTPMCPServer;
import cuspymd.mcp.mod.bridge.IPCServer;
import cuspymd.mcp.mod.config.MCPConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MCPServerModServer implements DedicatedServerModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("mcp-server-mod");
    private HTTPMCPServer httpServer;
    private IPCServer ipcServer;

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
                            new cuspymd.mcp.mod.server.tools.ServerScreenshotUtils()
                        );
                        httpServer.start();
                        LOGGER.info("HTTP MCP Server started on port {}", httpServer.getPort());
                    } else {
                        // Default to stdio transport
                        ipcServer = new IPCServer(config, new cuspymd.mcp.mod.server.tools.ServerCommandExecutor(config, server));
                        ipcServer.start();
                        LOGGER.info("IPC Server started on port {}", ipcServer.getPort());
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Failed to start MCP Server", e);
            }
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (ipcServer != null) {
                ipcServer.stop();
                LOGGER.info("IPC Server stopped");
            }
            if (httpServer != null) {
                httpServer.stop();
                LOGGER.info("HTTP MCP Server stopped");
            }
        });
    }
}