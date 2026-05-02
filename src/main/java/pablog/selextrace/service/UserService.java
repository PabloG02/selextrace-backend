package pablog.selextrace.service;

import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pablog.selextrace.dto.auth.AuthDtos;
import pablog.selextrace.model.auth.IdentityProvider;
import pablog.selextrace.model.persistence.*;
import pablog.selextrace.repository.AppUserRepository;
import pablog.selextrace.repository.UserIdentityRepository;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

// Used by ADMIN users
@Service
public class UserService {

    private final AppUserRepository userRepository;
    private final UserIdentityRepository userIdentityRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            AppUserRepository userRepository,
            UserIdentityRepository userIdentityRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.userIdentityRepository = userIdentityRepository;
        this.passwordEncoder = passwordEncoder;
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

        PasswordIdentityRecord identity = userIdentityRepository
                .findByUser_EmailAndProvider(user.getEmail(), IdentityProvider.PASSWORD)
                .filter(i -> i instanceof PasswordIdentityRecord)
                .map(i -> (PasswordIdentityRecord) i)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Local identity not found"));

        identity.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userIdentityRepository.save(identity);
        user.setMustChangePassword(true);
        userRepository.save(user);
        return toSummary(user);
    }

    private AuthDtos.UserSummaryResponse toSummary(AppUserRecord user) {
        return new AuthDtos.UserSummaryResponse(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getSystemRole(),
                user.isActive(),
                user.isMustChangePassword(),
                user.getCreatedAt()
        );
    }
}
