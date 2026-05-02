package pablog.selextrace.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pablog.selextrace.model.persistence.AppUserRecord;
import pablog.selextrace.repository.AppUserRepository;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class CurrentUserService {

    private final AppUserRepository userRepository;

    public CurrentUserService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public AuthenticatedUser requirePrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null
                || !auth.isAuthenticated()
                || auth instanceof AnonymousAuthenticationToken
                || !(auth.getPrincipal() instanceof AuthenticatedUser principal)) {
            throw new ResponseStatusException(UNAUTHORIZED, "Authentication required");
        }
        return principal;
    }

    public AppUserRecord requireUser() {
        return userRepository.findById(requirePrincipal().getUserId())
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "User no longer exists"));
    }
}