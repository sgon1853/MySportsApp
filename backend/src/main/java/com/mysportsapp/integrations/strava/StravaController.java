package com.mysportsapp.integrations.strava;

import com.mysportsapp.imports.ImportService;
import com.mysportsapp.imports.dto.ImportBatchResultDto;
import com.mysportsapp.integrations.strava.dto.StravaConnectResponseDto;
import com.mysportsapp.integrations.strava.dto.StravaStatusDto;
import com.mysportsapp.security.CurrentUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/integrations/strava")
public class StravaController {

    private final StravaOAuthService oAuthService;
    private final StravaSyncService syncService;
    private final StravaConnectionRepository connectionRepository;
    private final String frontendUrl;

    public StravaController(StravaOAuthService oAuthService, StravaSyncService syncService,
                             StravaConnectionRepository connectionRepository,
                             @Value("${app.strava.frontend-url}") String frontendUrl) {
        this.oAuthService = oAuthService;
        this.syncService = syncService;
        this.connectionRepository = connectionRepository;
        this.frontendUrl = frontendUrl;
    }

    /** Authenticated - the frontend calls this via a normal XHR (JWT attached
     * as usual) and navigates the browser to the returned URL itself. */
    @PostMapping("/connect")
    public ResponseEntity<StravaConnectResponseDto> connect() {
        UUID userId = CurrentUser.get().id();
        String authorizeUrl = oAuthService.buildAuthorizeUrl(userId);
        return ResponseEntity.ok(new StravaConnectResponseDto(authorizeUrl));
    }

    /** Public - this is where Strava's own redirect lands after the user
     * approves/denies access, as a plain browser navigation with no JWT
     * attached. {@code state} is what ties it back to the user who started
     * the flow (see {@link StravaOAuthService}). Always ends by redirecting
     * the browser back into the frontend, success or failure, rather than
     * rendering anything here directly. */
    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error) {

        String redirectPath = "/integrations/strava";
        String query;
        if (error != null) {
            query = "?stravaError=" + error;
        } else if (code == null || state == null) {
            query = "?stravaError=missing_code_or_state";
        } else {
            try {
                oAuthService.completeConnection(code, state);
                query = "?connected=true";
            } catch (RuntimeException e) {
                query = "?stravaError=connection_failed";
            }
        }

        URI redirectUri = UriComponentsBuilder.fromHttpUrl(frontendUrl).path(redirectPath).query(query.substring(1))
                .build().toUri();
        return ResponseEntity.status(HttpStatus.FOUND).location(redirectUri).build();
    }

    @GetMapping("/status")
    public ResponseEntity<StravaStatusDto> status() {
        UUID userId = CurrentUser.get().id();
        return connectionRepository.findByUserId(userId)
                .map(connection -> ResponseEntity.ok(new StravaStatusDto(true, connection.getConnectedAt())))
                .orElseGet(() -> ResponseEntity.ok(new StravaStatusDto(false, null)));
    }

    @PostMapping("/sync")
    public ResponseEntity<ImportBatchResultDto> sync() {
        UUID userId = CurrentUser.get().id();
        ImportService.Outcome outcome = syncService.sync(userId);
        return ResponseEntity.ok(outcome.result());
    }
}
