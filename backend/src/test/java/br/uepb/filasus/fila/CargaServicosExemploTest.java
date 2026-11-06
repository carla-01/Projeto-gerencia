package br.uepb.filasus.fila;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

/** Carga da fila de exemplo (US-06 - SCRUM-41). */
@SpringBootTest
@ActiveProfiles("test")
class CargaServicosExemploTest {

    @Autowired
    private ServicoRepository servicoRepository;

    @Autowired
    private SenhaRepository senhaRepository;

    @Autowired
    private ApplicationContext contexto;

    @BeforeEach
    @AfterEach
    void limparTabelas() {
        senhaRepository.deleteAll();
        servicoRepository.deleteAll();
    }

    /** No perfil "test" a carga fica desligada: os testes começam com o banco vazio. */
    @Test
    void naoRodaNoPerfilDeTeste() {
        assertTrue(contexto.getBeansOfType(CargaServicosExemplo.class).isEmpty());
    }

    @Test
    void criaOsTresServicosAtivos() {
        new CargaServicosExemplo(servicoRepository).run(null);

        List<String> nomes = servicoRepository.findAll().stream().map(Servico::getNome).sorted().toList();
        assertEquals(List.of("Clínico", "Enfermagem", "Odontologia"), nomes);
        assertTrue(servicoRepository.findAll().stream().allMatch(Servico::isAtivo));
    }

    @Test
    void naoDuplicaSeRodarDuasVezes() {
        CargaServicosExemplo carga = new CargaServicosExemplo(servicoRepository);

        carga.run(null);
        carga.run(null);

        assertEquals(3, servicoRepository.count());
    }

    @Test
    void naoReativaServicoQueOGestorDesativou() {
        Servico odontologia = new Servico("Odontologia");
        odontologia.setAtivo(false);
        servicoRepository.save(odontologia);

        new CargaServicosExemplo(servicoRepository).run(null);

        assertEquals(3, servicoRepository.count());
        assertFalse(servicoRepository.findByNomeAndAtivoTrue("Odontologia").isPresent());
    }
}
