package cuspymd.mcp.mod.server;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import cuspymd.mcp.mod.config.MCPConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class MCPServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(MCPServer.class);
    
    private final MCPConfig config;
    private HttpServer httpServer;
    private MCPRequestHandler requestHandler;
    
    public MCPServer(MCPConfig config) {
        this.config = config;
        this.requestHandler = new MCPRequestHandler(config);
    }
    
    public void start() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(
            config.getServer().getHost(), 
            config.getServer().getPort()
        ), 0);
        
        httpServer.createContext("/mcp/initialize", requestHandler::handleInitialize);
        httpServer.createContext("/mcp/ping", requestHandler::handlePing);
        httpServer.createContext("/mcp/tools/list", requestHandler::handleToolsList);
        httpServer.createContext("/mcp/tools/call", requestHandler::handleToolsCall);
        
        httpServer.setExecutor(Executors.newFixedThreadPool(4));
        httpServer.start();
        
        LOGGER.info("MCP Server started on {}:{}", 
            config.getServer().getHost(), 
            config.getServer().getPort());
    }
    
    public void stop() {
        if (httpServer != null) {
            httpServer.stop(1);
            LOGGER.info("MCP Server stopped");
        }
    }
}