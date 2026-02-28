package cuspymd.mcp.mod.utils;

import com.google.gson.JsonObject;

public interface IBlockScanner {
    JsonObject scanBlocksInArea(JsonObject fromPos, JsonObject toPos, int maxAreaSize);
}
