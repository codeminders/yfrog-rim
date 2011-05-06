package codeminders.yfrog.utils;

public class IntStringContainer {

    private int _intValue;
    private String _stringValue;

    public IntStringContainer(int i, String s) {
        _intValue = i;
        _stringValue = s;
    }

    public int intValue() { return _intValue; }
    public String toString() { return _stringValue; }
}

