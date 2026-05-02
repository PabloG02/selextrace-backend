package pablog.selextrace.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
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

    private final AppUserRepository userRepository;
    private final PasswordIdentityRepository passwordIdentityRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;

    public AuthService(
            AppUserRepository userRepository,
            PasswordIdentityRepository passwordIdentityRepository,
            AuthenticationManager authenticationManager,
            PasswordEncoder passwordEncoder,
            CurrentUserService currentUserService
    ) {
        this.userRepository = userRepository;
        this.passwordIdentityRepository = passwordIdentityRepository;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public AuthDtos.AuthUserResponse signup(
            AuthDtos.SignUpRequest request,
            HttpServletRequest httpRequest
    ) {
        String email = normalizeEmail(request.email());
        String username = normalizeUsername(request.username());
        String password = normalizePassword(request.password());

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(CONFLICT, "An account with this email already exists");
        }

        boolean firstUser = userRepository.count() == 0;

        AppUserRecord user = new AppUserRecord();
        user.setId(UUID.randomUUID().toString());
        user.setEmail(email);
        user.setUsername(username);
        user.setSystemRole(firstUser ? SystemRole.ADMIN : SystemRole.STANDARD);

        PasswordIdentityRecord identity = new PasswordIdentityRecord();
        identity.setPasswordHash(passwordEncoder.encode(password));
        user.addIdentity(identity);

        userRepository.save(user);

        authenticateAndPersistSession(email, password, httpRequest);
        return toAuthUserResponse(user);
    }

    public AuthDtos.AuthUserResponse signin(
            AuthDtos.SignInRequest request,
            HttpServletRequest httpRequest
    ) {
        String email = normalizeEmail(request.email());
        String password = normalizePassword(request.password());
        authenticateAndPersistSession(email, password, httpRequest);
        AppUserRecord user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found"));
        return toAuthUserResponse(user);
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        new SecurityContextLogoutHandler().logout(request, response, auth);
    }

    public AuthDtos.AuthUserResponse me() {
        AppUserRecord user = currentUserService.requireUser();
        return toAuthUserResponse(user);
    }

    @Transactional
    public AuthDtos.AuthUserResponse changePassword(AuthDtos.ChangePasswordRequest request) {
        if (request == null || request.currentPassword() == null || request.newPassword() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Current password and new password are required");
        }

        AppUserRecord user = currentUserService.requireUser();
        PasswordIdentityRecord identity = passwordIdentityRepository
                .findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No password login found for this account"));

        if (!passwordEncoder.matches(request.currentPassword(), identity.getPasswordHash())) {
            throw new ResponseStatusException(BAD_REQUEST, "Current password is incorrect");
        }

        identity.setPasswordHash(passwordEncoder.encode(normalizePassword(request.newPassword())));
        user.setMustChangePassword(false);
        userRepository.save(user);

        return toAuthUserResponse(user);
    }

    public AuthDtos.AuthUserResponse toAuthUserResponse(AppUserRecord user) {
        return new AuthDtos.AuthUserResponse(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getSystemRole(),
                user.isMustChangePassword(),
                user.getCreatedAt()
        );
    }

    private void authenticateAndPersistSession(
            String email,
            String password,
            HttpServletRequest httpRequest
    ) {
        var authRequest = UsernamePasswordAuthenticationToken.unauthenticated(email, password);
        var authentication = authenticationManager.authenticate(authRequest);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        httpRequest.getSession(true)
                .setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Email is required");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Display name is required");
        }
        return username.trim();
    }

    private String normalizePassword(String password) {
        if (password == null || password.length() < 10) {
            throw new ResponseStatusException(BAD_REQUEST, "Password must be at least 10 characters long");
        }
        return password;
    }
}
