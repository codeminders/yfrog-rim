package codeminders.yfrog.lib.network;

public interface NetworkListener {
    void started(long id);
    void complete(long id, Object result);
    void error(long id, Throwable ex);
    void cancelled(long id);
}

