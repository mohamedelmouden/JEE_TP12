package fst.elmouden.demonstration.repositories;

import fst.elmouden.demonstration.entities.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
}