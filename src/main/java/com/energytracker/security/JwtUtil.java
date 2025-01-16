package com.energytracker.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * @author André Heinen
 */
@Component
public class JwtUtil {

	private SecretKey key;

	@Value("${jwt.secret}")
	private String secretKey;

	@PostConstruct
	public void init() {
		key = Keys.hmacShaKeyFor(secretKey.getBytes());
	}

	public String extractUsername(String token) {
		return getClaimsFromToken(token).getSubject();
	}

	public Long extractUserId(String token) {
		return Long.parseLong(getClaimsFromToken(token).get("id").toString());
	}

	public Boolean isTokenValid(String token, String username) {
		String extractedUsername = extractUsername(token);
		return (extractedUsername.equals(username) && !isTokenExpired(token));
	}

	private Boolean isTokenExpired(String token) {
		return getClaimsFromToken(token)
				.getExpiration()
				.before(new Date());
	}

	public Claims getClaimsFromToken(String token) {
		return Jwts.parserBuilder()
				.setSigningKey(key)
				.build()
				.parseClaimsJws(token)
				.getBody();
	}

	public String extractRole(String token) {
		Claims claims = Jwts.parserBuilder()
				.setSigningKey(key)
				.build()
				.parseClaimsJws(token)
				.getBody();
		return (String) claims.get("role");
	}
}
