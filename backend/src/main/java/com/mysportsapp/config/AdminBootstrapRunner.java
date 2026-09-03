package com.mysportsapp.config;

import com.mysportsapp.user.User;
import com.mysportsapp.user.UserRepository;
import com.mysportsapp.user.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * On startup, if no ADMIN user exists yet, creates one from the
 * {@code ADMIN_BOOTSTRAP_EMAIL} / {@code ADMIN_BOOTSTRAP_PASSWORD} env vars.
 * This is the only way an ADMIN account ever comes into existence - there is
 * no public registration and no "first user becomes admin" magic. Idempotent:
 * safe to run on every startup.
 */
@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String bootstrapEmail;
    private final String bootstrapPassword;

    public AdminBootstrapRunner(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin-bootstrap.email:}") String bootstrapEmail,
            @Value("${app.admin-bootstrap.password:}") String bootstrapPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.bootstrapEmail = bootstrapEmail;
        this.bootstrapPassword = bootstrapPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(UserRole.ADMIN)) {
            return;
        }

        if (bootstrapEmail == null || bootstrapEmail.isBlank()
                || bootstrapPassword == null || bootstrapPassword.isBlank()) {
            log.warn("No ADMIN user exists and ADMIN_BOOTSTRAP_EMAIL/ADMIN_BOOTSTRAP_PASSWORD are not set; "
                    + "skipping admin bootstrap. Set both env vars and restart to create the first admin.");
            return;
        }

        User admin = new User(
                UUID.randomUUID(),
                bootstrapEmail,
                passwordEncoder.encode(bootstrapPassword),
                UserRole.ADMIN,
                true,
                null,
                null,
                null,
                Instant.now()
        );
        userRepository.save(admin);
        log.info("Bootstrapped initial ADMIN user '{}'", bootstrapEmail);
    }
}
