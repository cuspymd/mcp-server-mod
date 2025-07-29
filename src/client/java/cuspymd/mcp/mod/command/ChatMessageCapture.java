package cuspymd.mcp.mod.command;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class ChatMessageCapture {
    private static final ChatMessageCapture INSTANCE = new ChatMessageCapture();
    private final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
    private volatile boolean capturing = false;
    
    public static ChatMessageCapture getInstance() {
        return INSTANCE;
    }
    
    public void captureMessage(String message) {
        if (capturing && message != null) {
            messageQueue.offer(message);
        }
    }
    
    public void startCapturing() {
        capturing = true;
        messageQueue.clear();
    }
    
    public void stopCapturing() {
        capturing = false;
    }
    
    public String waitForMessage(long timeoutMs) throws InterruptedException {
        return messageQueue.poll(timeoutMs, TimeUnit.MILLISECONDS);
    }
    
    public String waitForMessage(long timeoutMs, Predicate<String> filter) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            String message = messageQueue.poll(100, TimeUnit.MILLISECONDS);
            if (message != null && filter.test(message)) {
                return message;
            }
        }
        return null;
    }
}