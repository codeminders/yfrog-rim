package codeminders.yfrog.lib.items;

import java.lang.*;

/**
 *
 */
public class Response extends YFrogItemBase {
    // error codes
    public final static int SUCCESS=0;
    public final static int INVALID_USERNAME_PASSWORD=1001; //Invalid twitter username or password
    public final static int IMAGE_NOT_FOUND=1002; // Image/video not found
    public final static int UNSUPPORTED_MEDIA_TYPE=1003; // Unsupported image/video type
    public final static int MEDIA_TOO_BIG=1004; // Image/video is too big

    public String stat;
    public int errCode;
    public String errMessage;

    // data fields
    public String mediaid;
    public String mediaurl;

    public Response() {
        super();
        errCode = SUCCESS;
    }
}
