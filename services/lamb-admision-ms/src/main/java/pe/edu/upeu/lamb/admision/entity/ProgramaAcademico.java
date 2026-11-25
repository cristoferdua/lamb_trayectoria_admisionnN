package pe.edu.upeu.lamb.admision.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "programas_academicos")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgramaAcademico {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String codigo;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String facultad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sede", nullable = false)
    private Sede sede;

    @Column(nullable = false)
    private Boolean activo;
}
