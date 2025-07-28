package cuspymd.mcp.mod;

import net.fabricmc.api.ClientModInitializer;
import cuspymd.mcp.mod.bridge.IPCServer;
import cuspymd.mcp.mod.bridge.HTTPMCPServer;
import cuspymd.mcp.mod.config.MCPConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MCPServerModClient implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("mcp-server-mod");
	private IPCServer ipcServer;
	private HTTPMCPServer httpServer;
	
	@Override
	public void onInitializeClient() {
		LOGGER.info("Initializing Minecraft MCP Client");
		
		try {
			MCPConfig config = MCPConfig.load();
			if (config.getServer().isAutoStart()) {
				String transport = config.getServer().getTransport();
				
				if ("http".equals(transport)) {
					httpServer = new HTTPMCPServer(config);
					httpServer.start();
					LOGGER.info("HTTP MCP Server started on port {}", httpServer.getPort());
				} else {
					// Default to stdio transport
					ipcServer = new IPCServer(config);
					ipcServer.start();
					LOGGER.info("IPC Server started on port {}", ipcServer.getPort());
				}
			}
		} catch (Exception e) {
			LOGGER.error("Failed to start MCP Server", e);
		}
	}
	
	public void onClientShutdown() {
		if (ipcServer != null) {
			ipcServer.stop();
			LOGGER.info("IPC Server stopped");
		}
		if (httpServer != null) {
			httpServer.stop();
			LOGGER.info("HTTP MCP Server stopped");
		}
	}
}