package pablog.selextrace.service;

import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pablog.selextrace.dto.auth.AuthDtos;
import pablog.selextrace.model.persistence.*;
import pablog.selextrace.repository.AppUserRepository;
import pablog.selextrace.repository.PasswordIdentityRepository;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

// Used by ADMIN users
@Service
public class UserService {

    private final AppUserRepository userRepository;
    private final PasswordIdentityRepository passwordIdentityRepository;
    private final PasswordEncoder passwordEncoder;
    private final IdentityProviderService identityProviderService;

    public UserService(
            AppUserRepository userRepository,
            PasswordIdentityRepository passwordIdentityRepository,
            PasswordEncoder passwordEncoder,
            IdentityProviderService identityProviderService
    ) {
        this.userRepository = userRepository;
        this.passwordIdentityRepository = passwordIdentityRepository;
        this.passwordEncoder = passwordEncoder;
        this.identityProviderService = identityProviderService;
    }

    public List<AuthDtos.UserSummaryResponse> listUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional
    public AuthDtos.UserSummaryResponse updateRole(String userId, AuthDtos.UpdateUserRoleRequest request) {
        if (request == null || request.systemRole() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "System role is required");
        }
        AppUserRecord user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found"));
        user.setSystemRole(request.systemRole());
        userRepository.save(user);

        return toSummary(user);
    }

    @Transactional
    public AuthDtos.UserSummaryResponse updateActive(String userId, AuthDtos.UpdateUserActiveRequest request) {
        AppUserRecord user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found"));
        user.setActive(request.active());
        userRepository.save(user);
        return toSummary(user);
    }

    @Transactional
    public AuthDtos.UserSummaryResponse resetPassword(String userId, AuthDtos.ResetPasswordRequest request) {
        if (request == null || request.newPassword() == null || request.newPassword().length() < 10) {
            throw new ResponseStatusException(BAD_REQUEST, "New password must be at least 10 characters long");
        }
        AppUserRecord user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found"));

        PasswordIdentityRecord identity = passwordIdentityRepository
                .findByUser_Email(user.getEmail())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Local identity not found"));

        identity.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        identity.setMustChangePassword(true);
        passwordIdentityRepository.save(identity);
        return toSummary(user);
    }

    private AuthDtos.UserSummaryResponse toSummary(AppUserRecord user) {
        return new AuthDtos.UserSummaryResponse(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getSystemRole(),
                user.isActive(),
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
}
