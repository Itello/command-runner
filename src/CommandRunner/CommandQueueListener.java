package CommandRunner;

public interface CommandQueueListener {
    void commandQueueStarted(int items);
    void commandQueueFinished();
    void commandQueueIsProcessing(Command command, int itemsLeft);
}
