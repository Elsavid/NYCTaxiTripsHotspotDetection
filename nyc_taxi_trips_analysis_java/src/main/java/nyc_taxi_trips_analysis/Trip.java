package nyc_taxi_trips_analysis;
import java.util.Objects;

public final class Trip {
    private long ts;
    private Location location;

    public Trip () {}

    public Trip(long ts, Location location) {
        this.ts = ts;
        this.location = location;
    }

    public long getTs() {
        return ts;
    }

    public void setTs(long ts) {
        this.ts = ts;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Trip trip = (Trip) o;
        return ts == trip.ts && location.equals(trip.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ts, location);
    }

    @Override
    public String toString() {
        return "Trip{" +
                "ts=" + ts +
                ", location=" + location +
                '}';
    }
}
