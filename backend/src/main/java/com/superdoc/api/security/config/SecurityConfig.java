package com.superdoc.api.config;

import com.superdoc.api.security.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // Enable @PreAuthorize and @PostAuthorize
public class SecurityConfig {

    private final JwtService jwt;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(JwtService jwt, CorsConfigurationSource corsConfigurationSource) {
        this.jwt = jwt;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // Enable CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                // APIs: no CSRF, no sessions
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Clear error codes (401 when unauthenticated, 403 when forbidden)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) ->
                                res.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                        .accessDeniedHandler((req, res, e) ->
                                res.sendError(HttpServletResponse.SC_FORBIDDEN))
                )

                // Authorization rules
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // preflight
                        .requestMatchers("/auth/**").permitAll()                // allow register/login
                        .requestMatchers("/api/auth/**").permitAll()            // allow register/login (API prefix)
                        .requestMatchers("/appointments/availability/**").permitAll() // anyone can check availability
                        .requestMatchers("/doctors").authenticated()          // anyone authenticated can list doctors
                        .requestMatchers("/doctors/**").hasAnyRole("DOCTOR")  // doctor endpoints require DOCTOR role
                        .requestMatchers("/patients/**").hasAnyRole("PATIENT", "DOCTOR") // patients can access their own, doctors can view patients
                        .requestMatchers("/appointments/**").authenticated()    // appointment endpoints require auth (checked in service)
                        .requestMatchers("/files/upload").authenticated()  // file uploads require auth
                        .requestMatchers("/files/**").permitAll()          // file downloads are public (or can be authenticated if needed)
                        .anyRequest().authenticated()
                )

                // JWT filter before username/password auth
                .addFilterBefore(new JwtAuthFilter(jwt), UsernamePasswordAuthenticationFilter.class)

                .build();
    }

    /**
     * Minimal JWT auth filter:
     * - Reads "Authorization: Bearer <token>"
     * - Parses with JwtService
     * - Sets Authentication with ROLE_<role> authority
     */
    static class JwtAuthFilter extends OncePerRequestFilter {
        private final JwtService jwt;

        JwtAuthFilter(JwtService jwt) {
            this.jwt = jwt;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest req,
                                        HttpServletResponse res,
                                        FilterChain chain)
                throws ServletException, IOException {

            String header = req.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);
                try {
                    var jws = jwt.parse(token); // expects subject=email and a "role" claim
                    var email = jws.getBody().getSubject();
                    var role = (String) jws.getBody().get("role");

                    var auth = new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } catch (Exception ignored) {
                    // Invalid/expired token -> leave context unauthenticated
                }
            }

            chain.doFilter(req, res);
        }
    }
}
