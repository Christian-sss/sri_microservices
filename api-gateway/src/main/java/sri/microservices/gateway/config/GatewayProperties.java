package sri.microservices.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sri.gateway")
public record GatewayProperties(
        String authServiceUrl,
        String cultivosServiceUrl,
        String sensoresServiceUrl,
        String riegoServiceUrl,
        String reportesServiceUrl,
        String chatServiceUrl
) {
}
