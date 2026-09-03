package com.workforce.vms.security;

import com.workforce.vms.config.DemoSecrets;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/** Issues and validates the session JWTs used for login + user attribution. */
@Service
public class JwtService {

    // DEMO-VULN: signing key is a hardcoded secret (see DemoSecrets).
    private final SecretKey key =
            Keys.hmacShaKeyFor(DemoSecrets.JWT_SIGNING_SECRET.getBytes(StandardCharsets.UTF_8));

    private static final long TTL_MS = 60 * 60 * 1000L; // 1 hour

    public String issueToken(String username, String email, String role) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(username)
                .claim("email", email)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + TTL_MS))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        Jws<Claims> jws = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
        return jws.getBody();
    }
}
