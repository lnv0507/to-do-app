package vn.com.anhemsoftware.license_app.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import vn.com.anhemsoftware.license_app.payload.auth.internal.GeoLocation;
import vn.com.anhemsoftware.license_app.service.GeoLocationService;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;

import org.springframework.data.redis.core.RedisTemplate;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class GeoLocationServiceImpl implements GeoLocationService {

    private final RestTemplate restTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    public GeoLocationServiceImpl(RedisTemplate<String, Object> redisTemplate) {
        this.restTemplate = new RestTemplate();
        this.redisTemplate = redisTemplate;
    }

    @Override
    public GeoLocation getLocation(String ip) {
        if (ip == null || isLocalOrPrivateIp(ip)) {
            return new GeoLocation("Local", "Local Network", true);
        }

        String cacheKey = "GEO_IP:" + ip;
        GeoLocation cached = (GeoLocation) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        try {
            // Using a free IP geolocation API
            String url = "http://ip-api.com/json/" + ip;
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && "success".equals(response.get("status"))) {
                String country = (String) response.get("country");
                String city = (String) response.get("city");
                GeoLocation result = new GeoLocation(country, city, false);
                redisTemplate.opsForValue().set(cacheKey, result, 7, TimeUnit.DAYS);
                return result;
            }
        } catch (Exception e) {
            log.warn("Failed to get geolocation for IP {}: {}", ip, e.getMessage());
        }

        // Fallback if API fails
        return new GeoLocation("Unknown", "Unknown", false);
    }

    private boolean isLocalOrPrivateIp(String ip) {
        return ip.equals("127.0.0.1") ||
                ip.equals("0:0:0:0:0:0:0:1") ||
                ip.startsWith("192.168.") ||
                ip.startsWith("10.") ||
                (ip.startsWith("172.") && isPrivate172(ip));
    }

    private boolean isPrivate172(String ip) {
        try {
            String[] parts = ip.split("\\.");
            int second = Integer.parseInt(parts[1]);
            return second >= 16 && second <= 31;
        } catch (Exception e) {
            return false;
        }
    }
}
