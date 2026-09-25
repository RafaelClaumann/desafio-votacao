package com.votacao.config;

import br.com.caelum.stella.format.CPFFormatter;
import br.com.caelum.stella.format.Formatter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ThirdPartyConfiguration {

    @Bean
    public Formatter cpfFormatter() {
        return new CPFFormatter();
    }

}
