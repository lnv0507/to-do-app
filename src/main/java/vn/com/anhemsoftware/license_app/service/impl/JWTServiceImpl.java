package vn.com.anhemsoftware.license_app.service.impl;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Jwts.SIG;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import vn.com.anhemsoftware.license_app.service.JWTService;

@Service
@RequiredArgsConstructor
public class JWTServiceImpl implements JWTService {
    public static final String SECRET = "5367566859703373367639792F423F452848284D6251655468576D5A71347437";
    // private static final long ACCESS_TOKEN_TTL_MS =
    // TimeUnit.MINUTES.toMillis(15);
    private static final long ACCESS_TOKEN_TTL_MS = TimeUnit.DAYS.toMillis(7);
    private static final long REFRESH_TOKEN_TTL_MS = TimeUnit.DAYS.toMillis(7);

    @Override
    public String generateToken(String email, UUID refreshTokenId) {
        HashMap<String, Object> claims = new HashMap<>();
        claims.put("jti", refreshTokenId.toString());
        return createToken(claims, email);
    }

    @Override
    public String createToken(Map<String, Object> claims, String email) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claims(claims)
                .subject(email)
                .issuedAt(new Date(now))
                .expiration(new Date(now + ACCESS_TOKEN_TTL_MS))
                .signWith(getSignInKey(), SIG.HS256)
                .compact();
    }

    @Override
    public String generateRefreshToken(String email, UUID refreshTokenId) {
        HashMap<String, Object> claims = new HashMap<>();
        claims.put("jti", refreshTokenId.toString());
        return createRefreshToken(claims, email);
    }

    @Override
    public String createRefreshToken(Map<String, Object> claims, String email) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claims(claims)
                .subject(email)
                .id(claims.get("jti").toString())
                .issuedAt(new Date(now))
                .expiration(new Date(now + REFRESH_TOKEN_TTL_MS))
                .signWith(getSignInKey(), SIG.HS256)
                .compact();
    }

    @Override
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    @Override
    public String extractJti(String token) {
        return extractClaim(token, Claims::getId);
    }

    @Override
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    @Override
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    @Override
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    @Override
    public SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    @Override
    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractEmail(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
}
