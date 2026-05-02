package pablog.selextrace.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pablog.selextrace.dto.auth.AuthDtos;
import pablog.selextrace.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/csrf")
    public AuthDtos.CsrfResponse csrf(CsrfToken csrfToken) {
        return new AuthDtos.CsrfResponse(
                csrfToken.getHeaderName(),
                csrfToken.getParameterName(),
                csrfToken.getToken()
        );
    }

    @PostMapping("/signup")
    public AuthDtos.AuthUserResponse signup(
            @RequestBody AuthDtos.SignUpRequest request,
            HttpServletRequest httpRequest
    ) {
        return authService.signup(request, httpRequest);
    }

    @PostMapping("/signin")
    public AuthDtos.AuthUserResponse signin(
            @RequestBody AuthDtos.SignInRequest request,
            HttpServletRequest httpRequest
    ) {
        return authService.signin(request, httpRequest);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request, response);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public AuthDtos.AuthUserResponse me() {
        return authService.me();
    }

    @PostMapping("/change-password")
    public AuthDtos.AuthUserResponse changePassword(@RequestBody AuthDtos.ChangePasswordRequest request) {
        return authService.changePassword(request);
    }
}
