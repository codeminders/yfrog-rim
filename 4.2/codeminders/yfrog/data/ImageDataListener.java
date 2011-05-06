package codeminders.yfrog.data;

import net.rim.device.api.system.EncodedImage;

public interface ImageDataListener {
    void imageLoaded(long requestID, String url, EncodedImage image);
}

