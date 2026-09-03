package com.mysportsapp.auth;

import com.mysportsapp.auth.dto.AcceptInviteRequest;
import com.mysportsapp.auth.dto.AuthResponse;
import com.mysportsapp.auth.dto.InviteRequest;
import com.mysportsapp.auth.dto.InviteResponse;
import com.mysportsapp.auth.dto.LoginRequest;
import com.mysportsapp.auth.dto.UserDto;
import com.mysportsapp.common.exception.InvalidCredentialsException;
import com.mysportsapp.common.exception.InvalidInviteException;
import com.mysportsapp.security.JwtService;
import com.mysportsapp.user.User;
import com.mysportsapp.user.UserRepository;
import com.mysportsapp.user.UserRole;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

@Service
public class AuthService {

    private static final int INVITE_EXPIRY_DAYS = 7;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .filter(User::isActive)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        return toAuthResponse(user);
    }

    @Transactional
    public InviteResponse invite(InviteRequest request, UUID invitedByAdminId) {
        Instant expiresAt = Instant.now().plus(INVITE_EXPIRY_DAYS, ChronoUnit.DAYS);
        String token = generateInviteToken();

        User user = userRepository.findByEmail(request.email()).orElse(null);
        if (user == null) {
            user = new User(
                    UUID.randomUUID(),
                    request.email(),
                    // placeholder hash - overwritten when the invite is accepted; never a usable credential
                    passwordEncoder.encode(UUID.randomUUID().toString()),
                    UserRole.USER,
                    false,
                    invitedByAdminId,
                    token,
                    expiresAt,
                    Instant.now()
            );
        } else {
            user.setInviteToken(token);
            user.setInviteTokenExpiresAt(expiresAt);
        }
        userRepository.save(user);

        return new InviteResponse(user.getEmail(), token, expiresAt.toString());
    }

    @Transactional
    public AuthResponse acceptInvite(AcceptInviteRequest request) {
        User user = userRepository.findByInviteToken(request.inviteToken())
                .orElseThrow(() -> new InvalidInviteException("Invalid invite token", false));

        if (user.getInviteTokenExpiresAt() == null || user.getInviteTokenExpiresAt().isBefore(Instant.now())) {
            throw new InvalidInviteException("Invite token has expired", true);
        }

        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setActive(true);
        user.setInviteToken(null);
        user.setInviteTokenExpiresAt(null);
        userRepository.save(user);

        return toAuthResponse(user);
    }

    private AuthResponse toAuthResponse(User user) {
        String token = jwtService.issueToken(user.getId(), user.getEmail(), user.getRole());
        UserDto dto = new UserDto(user.getId().toString(), user.getEmail(), user.getRole().name());
        return new AuthResponse(token, dto);
    }

    private String generateInviteToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
