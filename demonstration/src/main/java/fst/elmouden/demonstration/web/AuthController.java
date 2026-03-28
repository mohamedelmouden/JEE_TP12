package fst.elmouden.demonstration.web;

import fst.elmouden.demonstration.entities.Role;
import fst.elmouden.demonstration.entities.User;
import fst.elmouden.demonstration.jwt.JwtUtil;
import fst.elmouden.demonstration.repositories.RoleRepository;
import fst.elmouden.demonstration.repositories.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authenticationManager,
                          UserDetailsService userDetailsService,
                          JwtUtil jwtUtil,
                          UserRepository userRepository,
                          RoleRepository roleRepository,
                          PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService    = userDetailsService;
        this.jwtUtil               = jwtUtil;
        this.userRepository        = userRepository;
        this.roleRepository        = roleRepository;
        this.passwordEncoder       = passwordEncoder;
    }

    /**
     * Authentifie un utilisateur et retourne un token JWT.
     * Endpoint : POST /api/auth/login
     * Body     : { "username": "admin", "password": "1234" }
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {

        String username = request.get("username");
        String password = request.get("password");

        if (username == null || username.isBlank() ||
                password == null || password.isBlank()) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "error",     "Username et password sont obligatoires",
                            "timestamp", LocalDateTime.now().toString()
                    ));
        }

        try {
            // Verification des identifiants via Spring Security
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );

            // Chargement de l'utilisateur avec ses roles depuis la base de donnees
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // Generation du token JWT signe
            String token = jwtUtil.generateToken(userDetails);

            return ResponseEntity.ok(Map.of(
                    "token",     token,
                    "username",  username,
                    "type",      "Bearer",
                    "expiresIn", "3600s",
                    "roles",     userDetails.getAuthorities().toString(),
                    "timestamp", LocalDateTime.now().toString()
            ));

        } catch (BadCredentialsException e) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "error",     "Identifiants incorrects",
                            "timestamp", LocalDateTime.now().toString()
                    ));

        } catch (DisabledException e) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "error",     "Compte desactive, contactez l'administrateur",
                            "timestamp", LocalDateTime.now().toString()
                    ));
        }
    }

    /**
     * Cree un nouvel utilisateur avec le role ROLE_USER.
     * Endpoint : POST /api/auth/register
     * Body     : { "username": "alice", "password": "1234" }
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> request) {

        String username = request.get("username");
        String password = request.get("password");

        if (username == null || username.isBlank() ||
                password == null || password.isBlank()) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("error", "Username et password sont obligatoires"));
        }

        // Verification que le username n'existe pas deja
        if (userRepository.findByUsername(username).isPresent()) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Cet utilisateur existe deja"));
        }

        // Creation du role par defaut
        Role role = new Role();
        role.setName("ROLE_USER");
        roleRepository.save(role);

        // Creation de l'utilisateur avec mot de passe encode
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setActive(true);
        user.setRoles(List.of(role));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "message",   "Utilisateur cree avec succes",
                "username",  username,
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    /**
     * Verifie que l'API est en ligne.
     * Endpoint : GET /api/auth/status
     */
    @GetMapping("/status")
    public ResponseEntity<?> status() {
        return ResponseEntity.ok(Map.of(
                "status",    "API operationnelle",
                "app",       "spring-jwt-api",
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}