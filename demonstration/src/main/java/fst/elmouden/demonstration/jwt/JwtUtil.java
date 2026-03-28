package fst.elmouden.demonstration.jwt;


import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Component
public class JwtUtil {

    // Lecture depuis application.properties au lieu de hardcoder
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    // Clé cryptographique générée à partir du secret
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    
    //  GÉNÉRATION DU TOKEN
    
    /**
     * Génère un JWT contenant :
     * - le username comme "subject"
     * - les rôles de l'utilisateur dans les claims
     * - la date d'émission et d'expiration
     */
    public String generateToken(UserDetails userDetails) {

        // Extraire les rôles sous forme de liste de strings
        List<String> roles = userDetails.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return Jwts.builder()
                .setSubject(userDetails.getUsername())       // Identité de l'utilisateur
                .claim("roles", roles)                       //  Rôles embarqués dans le token
                .claim("app", "spring-jwt-api")              //  Identification de l'application
                .setIssuedAt(new Date())                     // Date de création
                .setExpiration(                              // Date d'expiration
                        new Date(System.currentTimeMillis() + expiration)
                )
                .signWith(getSigningKey(), SignatureAlgorithm.HS256) // Signature HMAC-SHA256
                .compact();
    }

    
    //  EXTRACTION DES INFORMATIONS DU TOKEN
    

    /**
     * Extrait tous les "claims" (données) du token.
     * C'est la méthode de base utilisée par les autres méthodes d'extraction.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Extrait le username (subject) depuis le token.
     * Exemple : "admin", "alice"
     */
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Extrait la date d'expiration du token.
     */
    public Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    /**
     * Extrait les rôles embarqués dans le token.
     * Exemple : ["ROLE_ADMIN", "ROLE_USER"]
     */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        return (List<String>) extractAllClaims(token).get("roles");
    }

    
    //  VALIDATION DU TOKEN
    

    /**
     * Vérifie si le token est expiré.
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Valide le token :
     * 1. Vérifie la signature (intégrité)
     * 2. Vérifie que le username correspond
     * 3. Vérifie que le token n'est pas expiré
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    /**
     * Validation simple (sans UserDetails) — utilisée dans le filtre.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            System.err.println(" Token expiré : " + e.getMessage());
        } catch (UnsupportedJwtException e) {
            System.err.println(" Token non supporté : " + e.getMessage());
        } catch (MalformedJwtException e) {
            System.err.println(" Token malformé : " + e.getMessage());
        } catch (SignatureException e) {
            System.err.println(" Signature invalide : " + e.getMessage());
        } catch (JwtException e) {
            System.err.println(" Erreur JWT : " + e.getMessage());
        }
        return false;
    }
}