package org.radar_project.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.jspecify.annotations.Nullable;

import java.net.InetSocketAddress;
import java.util.Optional;

/**
 * Optional HTTP proxy used for all outgoing requests (Ollama, GND, TIB).
 * <p>
 * If {@code host} is blank, connections are made directly. If a host is set, {@code port} is required.
 */
@ConfigurationProperties("radar.http.proxy")
public record ProxyProperties(@Nullable String host, @Nullable Integer port) {

    public ProxyProperties {
        host = (host == null || host.isBlank()) ? null : host.trim();
        if (host != null && (port == null || port < 1 || port > 65535)) {
            throw new IllegalArgumentException(
                    "radar.http.proxy.port must be between 1 and 65535 when radar.http.proxy.host is set");
        }
    }

    /**
     * @return the proxy address, or empty if no proxy is configured
     */
    public Optional<InetSocketAddress> address() {
        return host == null ? Optional.empty() : Optional.of(InetSocketAddress.createUnresolved(host, port));
    }
}
