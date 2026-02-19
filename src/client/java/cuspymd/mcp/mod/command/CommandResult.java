package cuspymd.mcp.mod.command;

import java.util.ArrayList;
import java.util.List;

public class CommandResult {
    private final boolean accepted;
    private final Boolean applied;
    private final String status;
    private final String summary;
    private final List<String> chatMessages;
    private final String originalCommand;
    private final long executionTimeMs;
    
    private CommandResult(Builder builder) {
        this.accepted = builder.accepted;
        this.applied = builder.applied;
        this.status = builder.status;
        this.summary = builder.summary;
        this.chatMessages = List.copyOf(builder.chatMessages);
        this.originalCommand = builder.originalCommand;
        this.executionTimeMs = builder.executionTimeMs;
    }
    
    public boolean isAccepted() { return accepted; }
    public Boolean getApplied() { return applied; }
    public String getStatus() { return status; }
    public String getSummary() { return summary; }
    public List<String> getChatMessages() { return chatMessages; }
    public String getOriginalCommand() { return originalCommand; }
    public long getExecutionTimeMs() { return executionTimeMs; }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private boolean accepted;
        private Boolean applied;
        private String status = "unknown";
        private String summary = "";
        private List<String> chatMessages = new ArrayList<>();
        private String originalCommand;
        private long executionTimeMs;
        
        public Builder accepted(boolean accepted) {
            this.accepted = accepted;
            return this;
        }
        
        public Builder applied(Boolean applied) {
            this.applied = applied;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder summary(String summary) {
            this.summary = summary;
            return this;
        }

        public Builder chatMessages(List<String> chatMessages) {
            this.chatMessages = chatMessages == null ? new ArrayList<>() : new ArrayList<>(chatMessages);
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
        
        public CommandResult build() {
            return new CommandResult(this);
        }
    }
}
