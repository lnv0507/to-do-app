package vn.com.anhemsoftware.license_app.service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

import io.jsonwebtoken.Claims;
import org.springframework.security.core.userdetails.UserDetails;


public interface JWTService {

    String generateToken(String email);
    String createToken(Map<String, Object> claims, String email);
    String generateRefreshToken(String email);
    String createRefreshToken(Map<String, Object> claims, String email);
    String extractEmail(String token);
    Date extractExpiration(String token);
    <T> T extractClaim(String token, Function<Claims, T> claimsResolver);
    Claims extractAllClaims(String token);
    SecretKey getSignInKey();
    boolean isTokenExpired(String token);
    boolean validateToken(String token, UserDetails userDetails);
    default boolean isTokenValid(String token) { return !isTokenExpired(token); }

}
