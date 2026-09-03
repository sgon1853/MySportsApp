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
 * <p>CORS matters for genuinely cross-origin deployments (e.g. two separate
 * Cloud Run services calling each other directly), but Spring's CORS filter
 * still inspects every request that carries an {@code Origin} header - and
 * browsers send one on same-origin POST/PUT/DELETE too (not just
 * cross-origin ones), including requests proxied same-origin through nginx
 * (docker-compose, local e2e runs). So the allowed-origin list has to cover
 * those proxied origins too, or Spring itself rejects them with 403 even
 * though the browser would have allowed the response through. Origin
 * *patterns* (wildcards) are used rather than exact origins so one default
 * covers every local port docker-compose/CI might publish the frontend on.
 */
@Configuration
public class SecurityConfig {

    private final ObjectMapper objectMapper;
    private final List<String> corsAllowedOriginPatterns;

    public SecurityConfig(
            ObjectMapper objectMapper,
            @Value("${app.cors.allowed-origin-patterns}") List<String> corsAllowedOriginPatterns) {
        this.objectMapper = objectMapper;
        this.corsAllowedOriginPatterns = corsAllowedOriginPatterns;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(corsAllowedOriginPatterns);
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
