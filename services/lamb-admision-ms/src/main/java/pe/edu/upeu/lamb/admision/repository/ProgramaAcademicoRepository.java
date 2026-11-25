package pe.edu.upeu.lamb.admision.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.lamb.admision.entity.ProgramaAcademico;

import java.util.List;

public interface ProgramaAcademicoRepository extends JpaRepository<ProgramaAcademico, Long> {
    List<ProgramaAcademico> findBySedeIdAndActivoTrueOrderByNombre(Long idSede);
    List<ProgramaAcademico> findByActivoTrueOrderByNombre();
}
