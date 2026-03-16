package vn.com.anhemsoftware.license_app.util;

import jakarta.servlet.http.HttpServletRequest;

public final class RequestUtils {
    private RequestUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty()) {
            return xfHeader.split(",")[0];
        }
        return request.getRemoteAddr();
    }

}
