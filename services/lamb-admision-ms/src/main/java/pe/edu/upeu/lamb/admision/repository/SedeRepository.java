package pe.edu.upeu.lamb.admision.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.lamb.admision.entity.Sede;

import java.util.List;

public interface SedeRepository extends JpaRepository<Sede, Long> {
    List<Sede> findByActivoTrueOrderByNombre();
}
