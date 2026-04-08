package vn.com.anhemsoftware.license_app.service;

import vn.com.anhemsoftware.license_app.payload.auth.internal.GeoLocation;

public interface GeoLocationService {
    GeoLocation getLocation(String ip);
}
