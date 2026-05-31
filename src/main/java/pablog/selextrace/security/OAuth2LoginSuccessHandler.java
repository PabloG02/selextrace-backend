package pablog.selextrace.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.stereotype.Component;
import pablog.selextrace.config.SecurityProperties;
import pablog.selextrace.model.persistence.AppUserRecord;
import pablog.selextrace.service.OAuth2UserProvisioningService;

import java.io.IOException;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginSuccessHandler.class);

    private final OAuth2UserProvisioningService provisioningService;
    private final SecurityProperties securityProperties;
    private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    public OAuth2LoginSuccessHandler(
            OAuth2UserProvisioningService provisioningService,
            SecurityProperties securityProperties
    ) {
        this.provisioningService = provisioningService;
        this.securityProperties = securityProperties;
    }

    @Override
    public void onAuthenticationSuccess(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        if (!(authentication.getPrincipal() instanceof OidcUser oidcUser)) {
            throw new ServletException("Expected an OIDC user from Google login");
        }

        AppUserRecord user = provisioningService.provisionGoogleUser(oidcUser);
        log.info("OAuth2 login succeeded: userId={}, email={}, role={}", user.getId(), user.getEmail(), user.getSystemRole());
        AuthenticatedUser principal = new AuthenticatedUser(
                user.getId(),
                user.getEmail(),
                null, // No password — identity is asserted by the OIDC provider
                user.getSystemRole(),
                user.isActive()
        );
        UsernamePasswordAuthenticationToken appAuthentication = UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                principal.getAuthorities()
        );
        appAuthentication.setDetails(authentication.getDetails());

        SecurityContext context = securityContextHolderStrategy.createEmptyContext();
        context.setAuthentication(appAuthentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        log.debug("Saved OAuth2 security context and redirecting to frontend success URL");
        redirectStrategy.sendRedirect(request, response, securityProperties.frontendSuccessUrl().toString());
    }
}
