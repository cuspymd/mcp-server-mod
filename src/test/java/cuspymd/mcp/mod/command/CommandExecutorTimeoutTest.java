package cuspymd.mcp.mod.command;

import cuspymd.mcp.mod.config.MCPConfig;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CommandExecutorTimeoutTest {

    @Test
    public void timeoutCancelsFutureAndReturnsTimedOut() {
        MCPConfig config = new MCPConfig();
        TimeoutFuture future = new TimeoutFuture();
        TestableCommandExecutor executor = new TestableCommandExecutor(config, future);

        CommandResult result = executor.executeCommandWithTimeout("say hello");

        assertEquals("timed_out", result.getStatus());
        assertFalse(result.isAccepted());
        assertTrue(future.cancelCalled, "Future should be cancelled on timeout");
    }

    @Test
    public void interruptionCancelsFutureAndPreservesInterruptFlag() {
        MCPConfig config = new MCPConfig();
        InterruptedFuture future = new InterruptedFuture();
        TestableCommandExecutor executor = new TestableCommandExecutor(config, future);

        try {
            CommandResult result = executor.executeCommandWithTimeout("say hello");
            assertEquals("execution_error", result.getStatus());
            assertEquals("Command execution interrupted", result.getSummary());
            assertTrue(future.cancelCalled, "Future should be cancelled on interruption");
            assertTrue(Thread.currentThread().isInterrupted(), "Interrupted flag should be preserved");
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    public void sendCommandIfPendingSkipsSenderWhenFutureAlreadyDone() {
        CompletableFuture<CommandResult> resultFuture = new CompletableFuture<>();
        resultFuture.cancel(true);
        AtomicInteger sentCount = new AtomicInteger();

        boolean sent = CommandExecutor.sendCommandIfPending(
            resultFuture,
            "say hello",
            cmd -> sentCount.incrementAndGet()
        );

        assertFalse(sent);
        assertEquals(0, sentCount.get());
    }

    @Test
    public void sendCommandIfPendingSendsWhenFutureIsPending() {
        CompletableFuture<CommandResult> resultFuture = new CompletableFuture<>();
        AtomicInteger sentCount = new AtomicInteger();

        boolean sent = CommandExecutor.sendCommandIfPending(
            resultFuture,
            "say hello",
            cmd -> sentCount.incrementAndGet()
        );

        assertTrue(sent);
        assertEquals(1, sentCount.get());
    }

    private static class TestableCommandExecutor extends CommandExecutor {
        private final CompletableFuture<CommandResult> future;

        private TestableCommandExecutor(MCPConfig config, CompletableFuture<CommandResult> future) {
            super(config);
            this.future = future;
        }

        @Override
        CompletableFuture<CommandResult> executeOneCommand(String command) {
            return future;
        }
    }

    private static class TimeoutFuture extends CompletableFuture<CommandResult> {
        private boolean cancelCalled = false;

        @Override
        public CommandResult get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
            throw new TimeoutException("forced timeout");
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            cancelCalled = true;
            return super.cancel(mayInterruptIfRunning);
        }
    }

    private static class InterruptedFuture extends CompletableFuture<CommandResult> {
        private boolean cancelCalled = false;

        @Override
        public CommandResult get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
            throw new InterruptedException("forced interrupt");
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            cancelCalled = true;
            return super.cancel(mayInterruptIfRunning);
        }
    }
}
