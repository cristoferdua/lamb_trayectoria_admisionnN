package pe.edu.upeu.lamb.evaluacionn.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pe.edu.upeu.lamb.evaluacionn.dto.ExamenRequest;
import pe.edu.upeu.lamb.evaluacionn.dto.ExamenResponse;
import pe.edu.upeu.lamb.evaluacionn.entity.Examen;

@Mapper(componentModel = "spring")
public interface ExamenMapper {

    ExamenResponse toResponse(Examen examen);

    @Mapping(target = "id", ignore = true)
    Examen toEntity(ExamenRequest request);
}
