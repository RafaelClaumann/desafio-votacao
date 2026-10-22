package com.votacao.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("DocumentoValidatorProperties")
class DocumentoValidatorPropertiesTest {

    @Test
    @DisplayName("Should bind app.documento-validator.is-enabled=true to the isEnabled property")
    void shouldBindIsEnabledToTrue_whenPropertyIsEnabled() {
        DocumentoValidatorProperties properties = bind(true);

        assertTrue(properties.isEnabled());
    }

    @Test
    @DisplayName("Should bind app.documento-validator.is-enabled=false to the isEnabled property")
    void shouldBindIsEnabledToFalse_whenPropertyIsDisabled() {
        DocumentoValidatorProperties properties = bind(false);

        assertFalse(properties.isEnabled());
    }

    private DocumentoValidatorProperties bind(boolean isEnabled) {
        Map<String, Object> source = new HashMap<>();
        source.put("app.documento-validator.base-url", "https://httpbin.org");
        source.put("app.documento-validator.connect-timeout-ms", "500");
        source.put("app.documento-validator.read-timeout-ms", "500");
        source.put("app.documento-validator.is-enabled", Boolean.toString(isEnabled));

        return new Binder(new MapConfigurationPropertySource(source))
                .bind("app.documento-validator", Bindable.of(DocumentoValidatorProperties.class))
                .get();
    }

}
