package com.signaltrellis.identity;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class IdentityUserDetailsService implements UserDetailsService {

    private final UserAccountRepository users;
    private final WorkspaceRepository workspaces;

    public IdentityUserDetailsService(UserAccountRepository users, WorkspaceRepository workspaces) {
        this.users = users;
        this.workspaces = workspaces;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserAccount user = users.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
        Workspace workspace = workspaces.findFirstByOwnerIdOrderById(user.getId())
                .orElseThrow(() -> new UsernameNotFoundException("Workspace not found"));
        return new AuthenticatedUser(user.getId(), workspace.getId(), user.getEmail(), user.getPasswordHash());
    }
}
