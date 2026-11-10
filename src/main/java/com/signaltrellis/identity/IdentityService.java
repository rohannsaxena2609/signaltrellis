package com.signaltrellis.identity;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;

@Service
public class IdentityService {

    private final UserAccountRepository users;
    private final WorkspaceRepository workspaces;
    private final PasswordEncoder passwordEncoder;

    public IdentityService(UserAccountRepository users, WorkspaceRepository workspaces,
                           PasswordEncoder passwordEncoder) {
        this.users = users;
        this.workspaces = workspaces;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthenticatedUser register(AuthController.RegisterRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new PasswordTooLongException();
        }
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException();
        }
        UserAccount user = users.saveAndFlush(new UserAccount(email, request.displayName().trim(),
                passwordEncoder.encode(request.password())));
        Workspace workspace = workspaces.save(new Workspace(user.getId(), "Personal"));
        return new AuthenticatedUser(user.getId(), workspace.getId(), user.getEmail(), user.getPasswordHash());
    }
}
