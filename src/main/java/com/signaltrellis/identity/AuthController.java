package com.signaltrellis.identity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final IdentityService identityService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(IdentityService identityService, AuthenticationManager authenticationManager,
                          JwtService jwtService) {
        this.identityService = identityService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return tokenResponse(identityService.register(request));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        validatePasswordLength(request.password());
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().trim().toLowerCase(), request.password()));
        return tokenResponse((AuthenticatedUser) authentication.getPrincipal());
    }

    @GetMapping("/me")
    public ProfileResponse profile(@AuthenticationPrincipal AuthenticatedUser user) {
        return new ProfileResponse(user.id(), user.workspaceId(), user.email());
    }

    private AuthResponse tokenResponse(AuthenticatedUser user) {
        return new AuthResponse("Bearer", jwtService.issue(user), jwtService.expirationSeconds(),
                user.id(), user.workspaceId(), user.email());
    }

    private void validatePasswordLength(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new PasswordTooLongException();
        }
    }

    public record RegisterRequest(@NotBlank @Email @Size(max = 320) String email,
                                  @NotBlank @Size(min = 10, max = 72) String password,
                                  @NotBlank @Size(max = 120) String displayName) { }

    public record LoginRequest(@NotBlank @Email @Size(max = 320) String email,
                               @NotBlank @Size(max = 72) String password) { }

    public record AuthResponse(String tokenType, String accessToken, long expiresInSeconds,
                               UUID userId, UUID workspaceId, String email) { }

    public record ProfileResponse(UUID userId, UUID workspaceId, String email) { }
}
