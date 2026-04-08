package vn.com.anhemsoftware.license_app.payload.auth.internal;

public record GeoLocation(
    String country,
    String city,
    boolean isLocal
) {
    public boolean isSameLocation(GeoLocation other) {
        if (other == null) return false;
        if (this.isLocal && other.isLocal) return true; // Local network => Same
        if (this.city != null && this.city.equals(other.city) && this.country != null && this.country.equals(other.country)) {
            return true;
        }
        return false;
    }
}
