package codeminders.yfrog.data.items;

import net.rim.device.api.util.Persistable;

public class UnsentStatusItem extends UnsentItemBase implements Persistable {

    public long InReplyToStatusID = 0;

    public UnsentStatusItem(
        String senderScreenName,
        String text,
        long inReplyToStatusID,
        String mediaFile,
        int locationOptions,
        Double latitude,
        Double longitude,
        String textLocationUrl
    ) {
        super(senderScreenName, text, mediaFile, locationOptions, latitude, longitude, textLocationUrl);
        this.InReplyToStatusID = inReplyToStatusID;
    }
}

