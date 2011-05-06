package codeminders.yfrog.data.items;

import net.rim.device.api.util.Persistable;

public class UnsentItemBase implements Persistable {

    public static final int LOCATION_TEXT = 1;
    public static final int LOCATION_MEDIA = 2;
    public static final int LOCATION_PROFILE = 4;

    private static final double LOCATION_DIVIDER = 1000000.0;

    public String SenderScreenName = null;
    public String Text = null;
    public String MediaFile = null;
    private int LocationOptions = 0;
    public String TextLocationUrl = null;

    // do not use Double direct, 4.2 issue
    private boolean hasCoordinates = false;
    private long latitude = 0L;
    private long longitude = 0L;

    protected UnsentItemBase(
        String senderScreenName,
        String text,
        String mediaFile,
        int locationOptions,
        Double latitude,
        Double longitude,
        String textLocationUrl
    ) {
        this.SenderScreenName = senderScreenName;
        this.Text = text;
        this.MediaFile = mediaFile;
        this.LocationOptions = locationOptions;
        this.TextLocationUrl = textLocationUrl;

        if ((latitude != null) && (longitude != null)) {
            this.hasCoordinates = true;
            this.latitude = (long)(latitude.doubleValue() * LOCATION_DIVIDER);
            this.longitude = (long)(longitude.doubleValue() * LOCATION_DIVIDER);
        }
        else
            this.hasCoordinates = false;
    }

    private Double getLatitude() {
        return hasCoordinates ? new Double((double)latitude / LOCATION_DIVIDER) : null;
    }
    private Double getLongitude() {
        return hasCoordinates ? new Double((double)longitude / LOCATION_DIVIDER) : null;
    }

    public boolean hasTextLocation() {
        return hasCoordinates
            && ((LocationOptions & LOCATION_TEXT) != 0)
            && (TextLocationUrl != null);
    }
    public Double getTextLatitude() {
        return ((LocationOptions & LOCATION_TEXT) != 0) ? getLatitude() : null;
    }
    public Double getTextLongitude() {
        return ((LocationOptions & LOCATION_TEXT) != 0) ? getLongitude() : null;
    }

    public Double getMediaLatitude() {
        return ((LocationOptions & LOCATION_MEDIA) != 0) ? getLatitude() : null;
    }
    public Double getMediaLongitude() {
        return ((LocationOptions & LOCATION_MEDIA) != 0) ? getLongitude() : null;
    }

    public boolean hasProfileLocation() {
        return hasCoordinates
            && ((LocationOptions & LOCATION_PROFILE) != 0);
    }
    public Double getProfileLatitude() {
        return ((LocationOptions & LOCATION_PROFILE) != 0) ? getLatitude() : null;
    }
    public Double getProfileLongitude() {
        return ((LocationOptions & LOCATION_PROFILE) != 0) ? getLongitude() : null;
    }

    public String toString() { return Text; }
}

