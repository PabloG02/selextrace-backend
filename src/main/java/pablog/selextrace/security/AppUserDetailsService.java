package pablog.selextrace.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(AppUserDetailsService.class);

    private final PasswordIdentityRepository passwordIdentityRepository;

    public AppUserDetailsService(PasswordIdentityRepository passwordIdentityRepository) {
        this.passwordIdentityRepository = passwordIdentityRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String normalizedEmail = username == null ? "" : username.trim().toLowerCase();
        log.debug("Loading password user by email={}", normalizedEmail);

        PasswordIdentityRecord identity = passwordIdentityRepository
                .findByUser_Email(normalizedEmail)
                .orElseThrow(() -> {
                    log.warn("Password login rejected for unknown email={}", normalizedEmail);
                    return new UsernameNotFoundException("Unknown account");
                });

        AppUserRecord user = identity.getUser();
        log.debug("Loaded password user: userId={}, email={}, active={}", user.getId(), user.getEmail(), user.isActive());

        return new AuthenticatedUser(
                user.getId(),
                user.getEmail(),
                identity.getPasswordHash(),
                user.getSystemRole(),
                user.isActive()
        );
    }
}