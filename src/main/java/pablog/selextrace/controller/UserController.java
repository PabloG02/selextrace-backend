package pablog.selextrace.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pablog.selextrace.dto.auth.AuthDtos;
import pablog.selextrace.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<AuthDtos.UserSummaryResponse> listUsers() {
        return userService.listUsers();
    }

    @PatchMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public AuthDtos.UserSummaryResponse updateRole(
            @PathVariable String userId,
            @RequestBody AuthDtos.UpdateUserRoleRequest request
    ) {
        return userService.updateRole(userId, request);
    }

    @PatchMapping("/{userId}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public AuthDtos.UserSummaryResponse updateActive(
            @PathVariable String userId,
            @RequestBody AuthDtos.UpdateUserActiveRequest request
    ) {
        return userService.updateActive(userId, request);
    }

    @PostMapping("/{userId}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public AuthDtos.UserSummaryResponse resetPassword(
            @PathVariable String userId,
            @RequestBody AuthDtos.ResetPasswordRequest request
    ) {
        return userService.resetPassword(userId, request);
    }
}
