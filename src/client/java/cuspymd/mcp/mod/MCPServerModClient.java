package cuspymd.mcp.mod;

import net.fabricmc.api.ClientModInitializer;
import cuspymd.mcp.mod.server.MCPServer;
import cuspymd.mcp.mod.config.MCPConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MCPServerModClient implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("mcp-server-mod");
	private MCPServer mcpServer;
	
	@Override
	public void onInitializeClient() {
		LOGGER.info("Initializing Minecraft MCP Client");
		
		try {
			MCPConfig config = MCPConfig.load();
			if (config.getServer().isAutoStart()) {
				mcpServer = new MCPServer(config);
				mcpServer.start();
				LOGGER.info("MCP Server started on port {}", config.getServer().getPort());
			}
		} catch (Exception e) {
			LOGGER.error("Failed to start MCP Server", e);
		}
	}
	
	public void onClientShutdown() {
		if (mcpServer != null) {
			mcpServer.stop();
			LOGGER.info("MCP Server stopped");
		}
	}
}