package codeminders.yfrog.utils;

public abstract class RunnableImpl implements Runnable {

    public Object data0 = null;
    public Object data1 = null;
    public int int0 = 0;

    public RunnableImpl() {
    }

    public RunnableImpl(Object data0) {
        this.data0 = data0;
    }

    public RunnableImpl(int int0) {
        this.int0 = int0;
    }

    public RunnableImpl(Object data0, Object data1) {
        this.data0 = data0;
        this.data1 = data1;
    }

    public RunnableImpl(Object data0, int int0) {
        this.data0 = data0;
        this.int0 = int0;
    }
}
