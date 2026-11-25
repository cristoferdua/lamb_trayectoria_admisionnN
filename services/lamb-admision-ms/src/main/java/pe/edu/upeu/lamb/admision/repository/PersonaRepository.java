package pe.edu.upeu.lamb.admision.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.lamb.admision.entity.Persona;

import java.util.Optional;

public interface PersonaRepository extends JpaRepository<Persona, Long> {
    Optional<Persona> findByDni(String dni);
    boolean existsByDni(String dni);
}
