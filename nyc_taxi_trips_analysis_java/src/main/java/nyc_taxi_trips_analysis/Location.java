package nyc_taxi_trips_analysis;

import java.util.Objects;

public final class Location {
    private double pos_x;
    private double pos_y;

    public Location() {}

    public Location(double pos_x, double pos_y) {
        this.pos_x = pos_x;
        this.pos_y = pos_y;
    }

    public double getPos_x() {
        return pos_x;
    }

    public void setPos_x(long pos_x) {
        this.pos_x = pos_x;
    }

    public double getPos_y() {
        return pos_y;
    }

    public void setPos_y(long pos_y) {
        this.pos_y = pos_y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Location location = (Location) o;
        return pos_x == location.pos_x && pos_y == location.pos_y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pos_x, pos_y);
    }

    @Override
    public String toString() {
        return "Location{" +
                "pos_x=" + pos_x +
                ", pos_y=" + pos_y +
                '}';
    }
}
