package cuspymd.mcp.mod.command;

public class CommandResult {
    private final boolean success;
    private final String message;
    private final String originalCommand;
    private final long executionTimeMs;
    private final int blocksAffected;
    private final int entitiesAffected;
    
    private CommandResult(Builder builder) {
        this.success = builder.success;
        this.message = builder.message;
        this.originalCommand = builder.originalCommand;
        this.executionTimeMs = builder.executionTimeMs;
        this.blocksAffected = builder.blocksAffected;
        this.entitiesAffected = builder.entitiesAffected;
    }
    
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getOriginalCommand() { return originalCommand; }
    public long getExecutionTimeMs() { return executionTimeMs; }
    public int getBlocksAffected() { return blocksAffected; }
    public int getEntitiesAffected() { return entitiesAffected; }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private boolean success;
        private String message;
        private String originalCommand;
        private long executionTimeMs;
        private int blocksAffected = 0;
        private int entitiesAffected = 0;
        
        public Builder success(boolean success) {
            this.success = success;
            return this;
        }
        
        public Builder message(String message) {
            this.message = message;
            return this;
        }
        
        public Builder originalCommand(String originalCommand) {
            this.originalCommand = originalCommand;
            return this;
        }
        
        public Builder executionTimeMs(long executionTimeMs) {
            this.executionTimeMs = executionTimeMs;
            return this;
        }
        
        public Builder blocksAffected(int blocksAffected) {
            this.blocksAffected = blocksAffected;
            return this;
        }
        
        public Builder entitiesAffected(int entitiesAffected) {
            this.entitiesAffected = entitiesAffected;
            return this;
        }
        
        public CommandResult build() {
            return new CommandResult(this);
        }
    }
}