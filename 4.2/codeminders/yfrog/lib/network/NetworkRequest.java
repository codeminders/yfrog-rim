package codeminders.yfrog.lib.network;

public interface NetworkRequest {
    long getID();
    int getPriority();
    int getHttpMethod();
    String getUrl();
    ArgumentsList getArgs();
    ContentProvider getPostContent();
    HttpConnectionPreprocessor getPreprocessor();
    ContentProcessor getContentProcessor();

    void startNext(
        int httpMethod, String url, ArgumentsList args, ContentProvider postContent,
        HttpConnectionPreprocessor preprocessor, ContentProcessor contentProcessor
    );
}

