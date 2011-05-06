package codeminders.yfrog.data.items;

import net.rim.device.api.util.Persistable;

public class UnsentDirectMessageItem extends UnsentItemBase implements Persistable {

    public String RecipientScreenName = null;

    public UnsentDirectMessageItem(
        String senderScreenName,
        String recipientScreenName,
        String text,
        String mediaFile,
        int locationOptions,
        Double latitude,
        Double longitude,
        String textLocationUrl
    ) {
        super(senderScreenName, text, mediaFile, locationOptions, latitude, longitude, textLocationUrl);
        this.RecipientScreenName = recipientScreenName;
    }
}

