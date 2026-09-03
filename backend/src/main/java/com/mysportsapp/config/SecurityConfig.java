package com.mysportsapp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysportsapp.common.exception.ErrorResponse;
import com.mysportsapp.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Stateless JWT-based security: no sessions, no CSRF (no cookies are used
 * for auth), the two auth endpoints are public, {@code /api/v1/admin/**}
 * requires ROLE_ADMIN, everything else requires authentication.
 *
 * <p>CORS only matters when the frontend is served from a different origin
 * than the backend (e.g. two separate Cloud Run services calling each other
 * directly). Same-origin deployments (docker-compose/nginx proxying "/api")
 * don't need it, but enabling it there is harmless - the browser only
 * consults CORS headers for cross-origin requests in the first place.
 */
@Configuration
public class SecurityConfig {

    private final ObjectMapper objectMapper;
    private final List<String> corsAllowedOrigins;

    public SecurityConfig(
            ObjectMapper objectMapper,
            @Value("${app.cors.allowed-origins}") List<String> corsAllowedOrigins) {
        this.objectMapper = objectMapper;
        this.corsAllowedOrigins = corsAllowedOrigins;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsAllowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        // Health/info must stay unauthenticated - Docker's healthcheck,
                        // Cloud Run's readiness checks, and deploy-time smoke tests all
                        // hit this directly with no credentials. Only health/info are
                        // exposed (see management.endpoints.web.exposure.include), so
                        // nothing sensitive is reachable here.
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                writeError(response, 401, "Authentication required"))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeError(response, 403, "Access denied")))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private void writeError(jakarta.servlet.http.HttpServletResponse response, int status, String message)
            throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(new ErrorResponse(message)));
    }
}
