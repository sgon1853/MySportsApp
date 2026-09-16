package com.mysportsapp.integrations.strava;

import com.mysportsapp.support.AbstractIntegrationTest;
import com.mysportsapp.user.User;
import com.mysportsapp.user.UserRepository;
import com.mysportsapp.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the encryption is actually happening at the database level, not
 * just that the Java-side round trip works (already covered by
 * {@link EncryptedStringConverterTest}) - reads the raw column value back
 * with plain JDBC, bypassing Hibernate/the converter entirely, the same way
 * an attacker with direct DB access would see it.
 */
class StravaConnectionEncryptionIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private StravaConnectionRepository connectionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void accessAndRefreshTokensAreNotStoredAsPlaintext() {
        User user = new User(UUID.randomUUID(), "strava-user+" + UUID.randomUUID() + "@example.com",
                "irrelevant-hash", UserRole.USER, true, null, null, null, Instant.now());
        userRepository.save(user);

        String plainAccessToken = "plaintext-access-token-should-not-appear-in-db";
        String plainRefreshToken = "plaintext-refresh-token-should-not-appear-in-db";
        StravaConnection connection = new StravaConnection(
                UUID.randomUUID(), user.getId(), 987654321L,
                plainAccessToken, plainRefreshToken, Instant.now().plusSeconds(21600), Instant.now());
        connectionRepository.save(connection);
        connectionRepository.flush();

        var row = jdbcTemplate.queryForMap(
                "SELECT access_token, refresh_token FROM strava_connections WHERE id = ?", connection.getId());

        assertThat((String) row.get("access_token")).doesNotContain(plainAccessToken);
        assertThat((String) row.get("refresh_token")).doesNotContain(plainRefreshToken);

        // The entity-level view (through the converter) still sees plaintext.
        StravaConnection reloaded = connectionRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(reloaded.getAccessToken()).isEqualTo(plainAccessToken);
        assertThat(reloaded.getRefreshToken()).isEqualTo(plainRefreshToken);
    }
}
