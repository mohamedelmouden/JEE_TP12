package fst.elmouden.demonstration.jwt;



import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtre HTTP execute une seule fois par requete.
 * Responsable de l'extraction, la validation du token JWT
 * et de l'injection de l'authentification dans le contexte Spring Security.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX        = "Bearer ";

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractToken(request);

        if (token != null) {
            processToken(token, request);
        }

        // Passage au filtre suivant dans la chaine Spring Security
        filterChain.doFilter(request, response);
    }

    /**
     * Extrait le token JWT depuis le header Authorization.
     * Retourne null si le header est absent ou ne commence pas par "Bearer ".
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);

        if (header != null && header.startsWith(BEARER_PREFIX)) {
            // Suppression du prefixe "Bearer " pour recuperer uniquement le token
            return header.substring(BEARER_PREFIX.length());
        }

        return null;
    }

    /**
     * Traite le token JWT extrait :
     * - Valide la signature et l'expiration
     * - Charge l'utilisateur depuis la base de donnees
     * - Injecte l'authentification dans le SecurityContext
     */
    private void processToken(String token, HttpServletRequest request) {

        String username = jwtUtil.extractUsername(token);
        System.out.println("DEBUG - username extrait : " + username);
        System.out.println("DEBUG - token valide : " + jwtUtil.validateToken(token));
        System.out.println("DEBUG - authentication null : " + (SecurityContextHolder.getContext().getAuthentication() == null));

        boolean isNotAuthenticated = SecurityContextHolder
                .getContext()
                .getAuthentication() == null;

        if (username != null && jwtUtil.validateToken(token) && isNotAuthenticated) {

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            System.out.println("DEBUG - roles : " + userDetails.getAuthorities());

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            System.out.println("DEBUG - authentification injectee avec succes");
        }
    }
}