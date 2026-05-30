package pablog.selextrace.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

@Configuration
public class OAuth2ClientConfig {

    @Bean
    @ConditionalOnProperty("selextrace.security.google-client-id")
    @ConditionalOnProperty("selextrace.security.google-client-secret")
    ClientRegistrationRepository clientRegistrationRepository(SecurityProperties securityProperties) {
        return new InMemoryClientRegistrationRepository(
                CommonOAuth2Provider.GOOGLE
                        .getBuilder("google")
                        .clientId(securityProperties.googleClientId())
                        .clientSecret(securityProperties.googleClientSecret())
                        .scope("openid", "email", "profile")
                        .build()
        );
    }
}
