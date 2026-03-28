package fst.elmouden.demonstration.config;



import lombok.RequiredArgsConstructor;
import fst.elmouden.demonstration.jwt.JwtAuthorizationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuration principale de Spring Security.
 * Definit les regles d'acces, la politique de session et l'ordre des filtres.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthorizationFilter jwtFilter;

    /**
     * Expose l'AuthenticationManager comme bean Spring.
     * Utilise par AuthController pour verifier les identifiants lors du login.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Encodeur de mot de passe utilisant l'algorithme BCrypt.
     * Utilise par Spring Security pour comparer les mots de passe lors de l'authentification.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Definition de la chaine de filtres de securite :
     * - Desactivation du CSRF (inutile en mode stateless)
     * - Politique de session STATELESS (aucune session HTTP cote serveur)
     * - Regles d'autorisation par route et par role
     * - Insertion du filtre JWT avant le filtre d'authentification standard
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // Desactivation du CSRF car l'API est stateless et utilise des tokens JWT
                .csrf(AbstractHttpConfigurer::disable)

                // Aucune session HTTP ne sera creee ou utilisee
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Definition des regles d'acces par route et par role
                .authorizeHttpRequests(auth -> auth
                        // Routes publiques : login et verification de statut
                        .requestMatchers("/api/auth/**").permitAll()
                        // Routes reservees aux administrateurs uniquement
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // Routes accessibles aux utilisateurs et aux administrateurs
                        .requestMatchers("/api/user/**").hasAnyRole("USER", "ADMIN")
                        // Toute autre route necessite une authentification
                        .anyRequest().authenticated()
                )

                // Insertion du filtre JWT avant le filtre d'authentification par formulaire
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}