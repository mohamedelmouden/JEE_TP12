package fst.elmouden.demonstration.config;

import fst.elmouden.demonstration.entities.Role;
import fst.elmouden.demonstration.entities.User;
import fst.elmouden.demonstration.repositories.RoleRepository;
import fst.elmouden.demonstration.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

/**
 * Initialisation des donnees au demarrage de l'application.
 * Cree les roles et les utilisateurs par defaut si la base est vide.
 */
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner initData() {
        return args -> {

            // Execution uniquement si la base est vide
            if (roleRepository.count() == 0) {

                // Creation des roles
                Role roleAdmin = new Role();
                roleAdmin.setName("ROLE_ADMIN");
                roleRepository.save(roleAdmin);

                Role roleUser = new Role();
                roleUser.setName("ROLE_USER");
                roleRepository.save(roleUser);

                // Creation de l'utilisateur admin
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("1234"));
                admin.setActive(true);
                admin.setRoles(List.of(roleAdmin));
                userRepository.save(admin);

                // Creation d'un utilisateur normal
                User user = new User();
                user.setUsername("user");
                user.setPassword(passwordEncoder.encode("1234"));
                user.setActive(true);
                user.setRoles(List.of(roleUser));
                userRepository.save(user);

                System.out.println("Donnees initialisees : admin et user crees avec succes");
            }
        };
    }
}