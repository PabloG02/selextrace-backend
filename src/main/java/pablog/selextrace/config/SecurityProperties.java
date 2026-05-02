package pablog.selextrace.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "selextrace.security")
public record SecurityProperties(List<String> allowedOrigins) {
    public SecurityProperties {
        allowedOrigins = allowedOrigins != null ? List.copyOf(allowedOrigins) : List.of();
    }
}
