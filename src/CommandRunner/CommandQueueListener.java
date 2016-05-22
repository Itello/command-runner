package CommandRunner;

public interface CommandQueueListener {
    void commandQueueStarted(CommandQueue commandQueue);
    void commandQueueFinished(CommandQueue commandQueue);
    void commandQueueIsProcessing(Command command);
}
