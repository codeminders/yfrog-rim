package codeminders.yfrog.twitter.items;

import codeminders.yfrog.utils.StringUtils;

public class UserLocation {

    private static final char[] SPACES = new char[] {
        0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
        0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F,
        0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17,
        0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x1F,
        ' ', ','
    };

    private String _text;
    private Double _latitude = null;
    private Double _longitude = null;

    public UserLocation(String location) {
        _text = (location != null) ? location.trim() : "";
        String[] values = StringUtils.split(_text, SPACES);
        if (values.length < 2)
            return;
        double lat, lon;
        try {
            lat = Double.parseDouble(values[values.length - 2]);
            lon = Double.parseDouble(values[values.length - 1]);
        }
        catch (Exception ex) {
            return;
        }
        _text = _text.substring(0, _text.indexOf(values[values.length - 2]));
        _latitude = new Double(lat);
        _longitude = new Double(lon);
    }

    public String getText() {
        return _text;
    }

    public boolean hasCoordinates() {
        return (_latitude != null) && (_longitude != null);
    }

    public double getLatitude() {
        return (_latitude != null) ? _latitude.doubleValue() : 0.0;
    }

    public double getLongitude() {
        return (_longitude != null) ? _longitude.doubleValue() : 0.0;
    }
}

