package pe.edu.upeu.lamb.evaluacionn.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "examenes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Examen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "titulo", nullable = false, length = 150)
    private String titulo;

    @Column(name = "curso", length = 100)
    private String curso;
}
