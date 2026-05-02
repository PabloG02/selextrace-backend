package pablog.selextrace.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pablog.selextrace.model.persistence.AppUserRecord;
import pablog.selextrace.model.persistence.PasswordIdentityRecord;
import pablog.selextrace.repository.PasswordIdentityRepository;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final PasswordIdentityRepository passwordIdentityRepository;

    public AppUserDetailsService(PasswordIdentityRepository passwordIdentityRepository) {
        this.passwordIdentityRepository = passwordIdentityRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String normalizedEmail = username == null ? "" : username.trim().toLowerCase();

        PasswordIdentityRecord identity = passwordIdentityRepository
                .findByUser_Email(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown account"));

        AppUserRecord user = identity.getUser();

        return new AuthenticatedUser(
                user.getId(),
                user.getEmail(),
                identity.getPasswordHash(),
                user.getSystemRole(),
                user.isActive()
        );
    }
}