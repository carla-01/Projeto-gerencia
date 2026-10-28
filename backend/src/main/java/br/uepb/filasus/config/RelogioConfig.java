package br.uepb.filasus.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Relógio usado pelas regras que dependem de horário (ex.: bloqueio do login).
 *
 * Em vez de chamar Instant.now() direto, as classes recebem este Clock.
 * Assim os testes conseguem trocar por um relógio simulado e "avançar" 15 minutos
 * sem precisar esperar de verdade.
 */
@Configuration
public class RelogioConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
