package TFG.Data_Analysis.Security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class TokenUtils {

    private final static String ACCES_TOKEN_SECRET = "pLuGfInDeRs3cUr17yC0d3f0rSiGnThE70k3nS"; //Secret key to sign the token
    private final static Long ACCESS_TOKEN_EXPIRATION_TIME = 2_592_000L; // 30 days in seconds, change it in production to a less time


    public static String generateAccessToken(Long userId, String email) {
        long expiration_time = ACCESS_TOKEN_EXPIRATION_TIME * 1_000;
        Date expiration_date = new Date(System.currentTimeMillis() + expiration_time);

        Map<String,Object> extra = new HashMap<>();
        extra.put("id", userId);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setExpiration(expiration_date)
                .addClaims(extra)
                .signWith(Keys.hmacShaKeyFor(ACCES_TOKEN_SECRET.getBytes()))
                .compact();
    }

    public static UsernamePasswordAuthenticationToken getAuthentication(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(ACCES_TOKEN_SECRET.getBytes()))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            String email = claims.getSubject();
            return new UsernamePasswordAuthenticationToken(email, null, Collections.emptyList());
        } catch (JwtException e) {
            return null; //Specifies that none UsernamePasswordAuthenticationToken has been created through this method.
        }
    }
}
