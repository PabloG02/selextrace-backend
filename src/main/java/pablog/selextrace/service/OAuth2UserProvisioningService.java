package pablog.selextrace.service;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import pablog.selextrace.model.auth.SystemRole;
import pablog.selextrace.model.persistence.AppUserRecord;
import pablog.selextrace.model.persistence.GoogleIdentityRecord;
import pablog.selextrace.repository.AppUserRepository;
import pablog.selextrace.repository.GoogleIdentityRepository;

import java.util.Locale;
import java.util.UUID;

@Service
public class OAuth2UserProvisioningService {

    private static final Logger log = LoggerFactory.getLogger(OAuth2UserProvisioningService.class);

    private final AppUserRepository userRepository;
    private final GoogleIdentityRepository googleIdentityRepository;

    public OAuth2UserProvisioningService(
            AppUserRepository userRepository,
            GoogleIdentityRepository googleIdentityRepository
    ) {
        this.userRepository = userRepository;
        this.googleIdentityRepository = googleIdentityRepository;
    }

    /// Provisions or loads a local user account for the given Google OIDC user.
    ///
    /// If a Google identity already exists for the subject, it is updated and the
    /// linked user is returned. Otherwise, the service looks up a user by email, or
    /// creates a new user and links the Google identity to it.
    ///
    /// @param oidcUser authenticated Google OIDC user
    /// @return the linked or newly created application user
    /// @throws OAuth2AuthenticationException if the email is missing, unverified, or the account is disabled
    @Transactional
    public AppUserRecord provisionGoogleUser(OidcUser oidcUser) {
        String subject = oidcUser.getSubject();
        String email = normalizeEmail(oidcUser.getEmail());
        requireVerifiedEmail(oidcUser);

        log.debug("Provisioning Google user: subject={}, email={}", subject, email);

        return googleIdentityRepository.findByProviderSubject(subject)
                .map(this::resolveExistingIdentity)
                .orElseGet(() -> linkOrCreateGoogleUser(oidcUser, subject, email));
    }

    /// Resolves an existing Google identity to its linked user account.
    ///
    /// @param identity existing Google identity record
    /// @return the linked user account
    private AppUserRecord resolveExistingIdentity(GoogleIdentityRecord identity) {
        AppUserRecord user = identity.getUser();
        assertActive(user);
        log.debug("Resolved existing Google identity: subject={}, userId={}, email={}", identity.getProviderSubject(), user.getId(), user.getEmail());
        return user;
    }

    /// Links the Google identity to an existing local user, or creates a new user first.
    ///
    /// @param oidcUser authenticated Google OIDC user
    /// @param subject Google provider subject
    /// @param email normalized email address
    /// @return the linked or newly created application user
    private AppUserRecord linkOrCreateGoogleUser(OidcUser oidcUser, String subject, String email) {
        var existingUser = userRepository.findByEmail(email);
        AppUserRecord user = existingUser.orElseGet(() -> {
            AppUserRecord createdUser = createUser(oidcUser, email);
            log.info("Created local account from Google login: userId={}, email={}, role={}", createdUser.getId(), email, createdUser.getSystemRole());
            return createdUser;
        });
        assertActive(user);

        GoogleIdentityRecord identity = new GoogleIdentityRecord();
        identity.setProviderSubject(subject);

        user.addIdentity(identity);
        userRepository.save(user);

        if (existingUser.isPresent()) {
            log.info("Linked Google identity to existing account: userId={}, email={}", user.getId(), email);
        }

        log.debug("Persisted Google identity link: subject={}, userId={}, email={}", subject, user.getId(), email);

        return user;
    }

    /// Creates a new application user from Google profile data.
    ///
    /// The first user in the system is granted the admin role; later users receive
    /// the standard role.
    ///
    /// @param oidcUser authenticated Google OIDC user
    /// @param email normalized email address
    /// @return a new user entity, not yet persisted
    private AppUserRecord createUser(OidcUser oidcUser, String email) {
        AppUserRecord user = new AppUserRecord();
        user.setId(UUID.randomUUID().toString());
        user.setEmail(email);
        user.setUsername(uniqueUsername(displayName(oidcUser, email)));
        user.setSystemRole(userRepository.count() == 0 ? SystemRole.ADMIN : SystemRole.STANDARD);
        return user;
    }

    /// Derives a display name from the OIDC profile.
    ///
    /// Falls back to the local part of the email address when no full name is present.
    ///
    /// @param oidcUser authenticated Google OIDC user
    /// @param email normalized email address
    /// @return a readable display name
    private String displayName(OidcUser oidcUser, String email) {
        String name = oidcUser.getFullName();
        if (!StringUtils.hasText(name)) {
            name = email.substring(0, email.indexOf('@'));
        }
        return name.trim();
    }

    /// Produces a username that is readable and unique within the system.
    ///
    /// @param candidate preferred username base
    /// @return a unique username
    private String uniqueUsername(String candidate) {
        String base = candidate.replaceAll("\\s+", " ").trim();
        if (base.isBlank()) {
            base = "Google user";
        }
        if (base.length() > 44) {
            base = base.substring(0, 44).trim();
        }

        String username = base;
        int suffix = 2;
        while (userRepository.existsByUsername(username)) {
            String suffixText = " " + suffix++;
            int maxBaseLength = 50 - suffixText.length();
            username = base.substring(0, Math.min(base.length(), maxBaseLength)).trim() + suffixText;
        }

        return username;
    }

    /// Normalizes and validates an email address.
    ///
    /// @param email raw email address from Google
    /// @return normalized email address
    /// @throws OAuth2AuthenticationException if the email is missing or blank
    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw oauthFailure("google_email_missing", "Google account did not include an email");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    /// Ensures that the Google account email is verified.
    ///
    /// @param oidcUser authenticated Google OIDC user
    /// @throws OAuth2AuthenticationException if the email is not verified
    private void requireVerifiedEmail(OidcUser oidcUser) {
        if (!Boolean.TRUE.equals(oidcUser.getEmailVerified())) {
            throw oauthFailure("google_email_not_verified", "Google account email is not verified");
        }
    }

    /// Ensures that the application user account is active.
    ///
    /// @param user application user to validate
    /// @throws OAuth2AuthenticationException if the account is disabled
    private void assertActive(AppUserRecord user) {
        if (!user.isActive()) {
            throw oauthFailure("account_disabled", "This account is disabled");
        }
    }

    /// Creates a standardized OAuth2 authentication error.
    ///
    /// @param code machine-readable error code
    /// @param description human-readable error description
    /// @return authentication exception suitable for OAuth2 flows
    private OAuth2AuthenticationException oauthFailure(String code, String description) {
        log.warn("Rejected Google login: code={}, description={}", code, description);
        return new OAuth2AuthenticationException(new OAuth2Error(code, description, null));
    }
}
