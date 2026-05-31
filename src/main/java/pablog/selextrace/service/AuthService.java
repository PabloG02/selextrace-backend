package pablog.selextrace.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import pablog.selextrace.dto.auth.AuthDtos;
import pablog.selextrace.model.auth.SystemRole;
import pablog.selextrace.model.persistence.AppUserRecord;
import pablog.selextrace.model.persistence.PasswordIdentityRecord;
import pablog.selextrace.repository.AppUserRepository;
import pablog.selextrace.repository.PasswordIdentityRepository;
import pablog.selextrace.security.CurrentUserService;

import java.util.Locale;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AppUserRepository userRepository;
    private final PasswordIdentityRepository passwordIdentityRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;
    private final IdentityProviderService identityProviderService;

    public AuthService(
            AppUserRepository userRepository,
            PasswordIdentityRepository passwordIdentityRepository,
            AuthenticationManager authenticationManager,
            PasswordEncoder passwordEncoder,
            CurrentUserService currentUserService,
            IdentityProviderService identityProviderService
    ) {
        this.userRepository = userRepository;
        this.passwordIdentityRepository = passwordIdentityRepository;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.currentUserService = currentUserService;
        this.identityProviderService = identityProviderService;
    }

    @Transactional
    public AuthDtos.AuthUserResponse signup(
            AuthDtos.SignUpRequest request,
            HttpServletRequest httpRequest
    ) {
        log.info("Signup attempt for email={}", request.email());

        String email = normalizeEmail(request.email());
        String username = normalizeUsername(request.username());
        String password = normalizePassword(request.password());

        if (userRepository.existsByEmail(email)) {
            log.warn("Signup rejected — email already in use: email={}", email);
            throw new ResponseStatusException(CONFLICT, "An account with this email already exists");
        }

        boolean firstUser = userRepository.count() == 0;
        SystemRole role = firstUser ? SystemRole.ADMIN : SystemRole.STANDARD;
        log.debug("Assigning role={} to new user email={} (firstUser={})", role, email, firstUser);

        AppUserRecord user = new AppUserRecord();
        user.setId(UUID.randomUUID().toString());
        user.setEmail(email);
        user.setUsername(username);
        user.setSystemRole(role);

        PasswordIdentityRecord identity = new PasswordIdentityRecord();
        identity.setPasswordHash(passwordEncoder.encode(password));
        user.addIdentity(identity);

        userRepository.save(user);
        log.info("User created successfully: userId={}, email={}, role={}", user.getId(), email, role);

        authenticateAndPersistSession(email, password, httpRequest);
        return toAuthUserResponse(user);
    }

    public AuthDtos.AuthUserResponse signin(
            AuthDtos.SignInRequest request,
            HttpServletRequest httpRequest
    ) {
        log.info("Sign In attempt for email={}", request.email());

        String email = normalizeEmail(request.email());
        String password = normalizePassword(request.password());
        authenticateAndPersistSession(email, password, httpRequest);

        AppUserRecord user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Sign In failed — user not found after authentication: email={}", email);
                    return new ResponseStatusException(NOT_FOUND, "User not found");
                });

        log.info("Sign In successful: userId={}, email={}", user.getId(), email);
        return toAuthUserResponse(user);
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("Logout for principal={}", auth != null ? auth.getName() : null);
        new SecurityContextLogoutHandler().logout(request, response, auth);
        log.debug("Session invalidated for principal={}", auth != null ? auth.getName() : null);
    }

    public AuthDtos.AuthUserResponse me() {
        AppUserRecord user = currentUserService.requireUser();
        log.debug("Current user fetched: userId={}, email={}", user.getId(), user.getEmail());
        return toAuthUserResponse(user);
    }

    @Transactional
    public AuthDtos.AuthUserResponse changePassword(AuthDtos.ChangePasswordRequest request) {
        if (request == null || request.currentPassword() == null || request.newPassword() == null) {
            log.warn("Change password rejected — missing required fields");
            throw new ResponseStatusException(BAD_REQUEST, "Current password and new password are required");
        }

        AppUserRecord user = currentUserService.requireUser();
        log.info("Password change attempt: userId={}, email={}", user.getId(), user.getEmail());

        PasswordIdentityRecord identity = passwordIdentityRepository
                .findByUser(user)
                .orElseThrow(() -> {
                    log.warn("Password change failed — no password identity found: userId={}", user.getId());
                    return new ResponseStatusException(NOT_FOUND, "No password login found for this account");
                });

        if (!passwordEncoder.matches(request.currentPassword(), identity.getPasswordHash())) {
            log.warn("Password change failed — incorrect current password: userId={}", user.getId());
            throw new ResponseStatusException(BAD_REQUEST, "Current password is incorrect");
        }

        identity.setPasswordHash(passwordEncoder.encode(normalizePassword(request.newPassword())));
        identity.setMustChangePassword(false);
        passwordIdentityRepository.save(identity);

        log.info("Password changed successfully: userId={}", user.getId());
        return toAuthUserResponse(user);
    }

    public AuthDtos.AuthUserResponse toAuthUserResponse(AppUserRecord user) {
        return new AuthDtos.AuthUserResponse(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getSystemRole(),
                user.getIdentities().stream()
                        .filter(PasswordIdentityRecord.class::isInstance)
                        .map(PasswordIdentityRecord.class::cast)
                        .findFirst()
                        .map(PasswordIdentityRecord::getMustChangePassword)
                        .orElse(false),
                identityProviderService.linkedProviders(user),
                user.getCreatedAt()
        );
    }

    private void authenticateAndPersistSession(
            String email,
            String password,
            HttpServletRequest httpRequest
    ) {
        log.debug("Authenticating and persisting session for email={}", email);
        var authRequest = UsernamePasswordAuthenticationToken.unauthenticated(email, password);
        var authentication = authenticationManager.authenticate(authRequest);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        httpRequest.getSession(true)
                .setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        log.debug("Session persisted for email={}", email);
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            log.debug("Email normalization failed — blank or null value");
            throw new ResponseStatusException(BAD_REQUEST, "Email is required");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeUsername(String username) {
        if (!StringUtils.hasText(username)) {
            log.debug("Username normalization failed — blank or null value");
            throw new ResponseStatusException(BAD_REQUEST, "Display name is required");
        }
        return username.trim();
    }

    private String normalizePassword(String password) {
        if (password == null || password.length() < 10) {
            log.debug("Password normalization failed — too short or null");
            throw new ResponseStatusException(BAD_REQUEST, "Password must be at least 10 characters long");
        }
        return password;
    }
}
