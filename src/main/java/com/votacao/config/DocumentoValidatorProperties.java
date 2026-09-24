package com.votacao.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.documento-validator")
public record DocumentoValidatorProperties(
        String baseUrl,
        int connectTimeoutMs,
        int readTimeoutMs
) {
}
