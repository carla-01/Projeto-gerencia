package br.uepb.filasus.bdd;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import io.cucumber.spring.CucumberContextConfiguration;

/** Sobe a aplicação com o perfil de teste (H2) para os passos do Cucumber. */
@CucumberContextConfiguration
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(CucumberSpringConfig.RelogioDaUbsDeTeste.class)
public class CucumberSpringConfig {

    /**
     * Relógio parado às 09:00 no horário da UBS (12:00 UTC), para os cenários da fila (US06)
     * não dependerem da hora em que o CI roda.
     */
    @TestConfiguration
    static class RelogioDaUbsDeTeste {

        @Bean
        @Primary
        Clock relogioDaUbsDeTeste() {
            return Clock.fixed(Instant.parse("2026-11-10T12:00:00Z"), ZoneId.of("America/Fortaleza"));
        }
    }
}
