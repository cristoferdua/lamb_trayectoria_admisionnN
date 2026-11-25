package pe.edu.upeu.lamb.admision.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    // Segunda linea: valida la firma del JWT por su cuenta. Las reglas por rol viven en el Gateway.
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // DEV Fase 1: todo publico hasta activar Keycloak en Fase 2
                        .anyRequest().permitAll()
                );
        return http.build();
    }
}
