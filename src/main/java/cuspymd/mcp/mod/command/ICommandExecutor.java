package cuspymd.mcp.mod.command;

import com.google.gson.JsonObject;

public interface ICommandExecutor {
    JsonObject executeCommands(JsonObject arguments);
}
