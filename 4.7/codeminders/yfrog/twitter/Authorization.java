package codeminders.yfrog.twitter;

import codeminders.yfrog.lib.network.ArgumentsList;
import codeminders.yfrog.lib.network.HttpConnectionPreprocessor;

public interface Authorization extends HttpConnectionPreprocessor {
    void applyToArgs(int httpMethod, String url, ArgumentsList args);
}

