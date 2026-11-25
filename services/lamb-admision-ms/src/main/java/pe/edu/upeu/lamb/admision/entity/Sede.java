package pe.edu.upeu.lamb.admision.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sedes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Sede {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false)
    private Boolean activo;
}
