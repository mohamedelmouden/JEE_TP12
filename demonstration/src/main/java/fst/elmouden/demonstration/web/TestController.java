package fst.elmouden.demonstration.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controleur de test pour verifier les regles d'autorisation par role.
 */
@RestController
public class TestController {

    /**
     * Endpoint accessible aux utilisateurs avec ROLE_USER ou ROLE_ADMIN.
     */
    @GetMapping("/api/user/profile")
    public ResponseEntity<?> userProfile() {
        return ResponseEntity.ok("Acces autorise — ROLE_USER");
    }

    /**
     * Endpoint accessible uniquement aux utilisateurs avec ROLE_ADMIN.
     */
    @GetMapping("/api/admin/dashboard")
    public ResponseEntity<?> adminDashboard() {
        return ResponseEntity.ok("Acces autorise — ROLE_ADMIN");
    }
}