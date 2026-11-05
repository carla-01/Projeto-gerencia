package br.uepb.filasus.bdd;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import io.cucumber.spring.CucumberContextConfiguration;

/** Sobe a aplicação com o perfil de teste (H2) para os passos do Cucumber. */
@CucumberContextConfiguration
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CucumberSpringConfig {
}
