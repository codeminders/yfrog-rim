package codeminders.yfrog.data;

import javax.microedition.location.Location;

public interface LocationDataListener {
    void locationReceived(Location location);
    void locationError(Throwable ex);
    void locationCancelled();
}

