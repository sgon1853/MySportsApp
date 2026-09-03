package com.mysportsapp.auth;

import com.mysportsapp.auth.dto.InviteRequest;
import com.mysportsapp.auth.dto.InviteResponse;
import com.mysportsapp.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only endpoints. Access is additionally enforced by {@code
 * SecurityConfig} requiring ROLE_ADMIN on all of {@code /api/v1/admin/**}.
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AuthService authService;

    public AdminController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/invite")
    public ResponseEntity<InviteResponse> invite(@Valid @RequestBody InviteRequest request) {
        InviteResponse response = authService.invite(request, CurrentUser.get().id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
