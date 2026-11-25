package pe.edu.upeu.lamb.admision.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Datos personales minimos del humano que postula. Dueno temporal: Admision,
 * hasta que exista el ms Personas (Base Universal). El acceso SIEMPRE va por
 * PersonaPort; el dia que llegue Personas real, se cambia el adaptador y se
 * migran estas filas al modulo oficial.
 */
@Entity
@Table(name = "personas_admision", uniqueConstraints = @UniqueConstraint(columnNames = "dni"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Persona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 15)
    private String dni;

    @Column(length = 100)
    private String nombres;

    @Column(name = "apellido_paterno", length = 100)
    private String apellidoPaterno;

    @Column(name = "apellido_materno", length = 100)
    private String apellidoMaterno;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(length = 20)
    private String sexo;

    @Column(length = 100)
    private String correo;

    @Column(length = 20)
    private String telefono;

    @Column(name = "fecha_registro", nullable = false)
    private Instant fechaRegistro;
}
