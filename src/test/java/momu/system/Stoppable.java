package momu.system;

public interface Stoppable {
    void stop();
    void awaitShutdown() throws InterruptedException;
}
