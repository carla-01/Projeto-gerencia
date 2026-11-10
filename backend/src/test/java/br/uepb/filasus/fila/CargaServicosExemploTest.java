package br.uepb.filasus.fila;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalTime;
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
    private ConfiguracaoServicoRepository configuracaoServicoRepository;

    @Autowired
    private ApplicationContext contexto;

    @BeforeEach
    @AfterEach
    void limparTabelas() {
        senhaRepository.deleteAll();
        configuracaoServicoRepository.deleteAll();
        servicoRepository.deleteAll();
    }

    /** No perfil "test" a carga fica desligada: os testes começam com o banco vazio. */
    @Test
    void naoRodaNoPerfilDeTeste() {
        assertTrue(contexto.getBeansOfType(CargaServicosExemplo.class).isEmpty());
    }

    @Test
    void criaOsTresServicosAtivos() {
        new CargaServicosExemplo(servicoRepository, configuracaoServicoRepository).run(null);

        List<String> nomes = servicoRepository.findAll().stream().map(Servico::getNome).sorted().toList();
        assertEquals(List.of("Clínico", "Enfermagem", "Odontologia"), nomes);
        assertTrue(servicoRepository.findAll().stream().allMatch(Servico::isAtivo));
        assertEquals(3, configuracaoServicoRepository.count());
        assertEquals(40, limiteDe("Clínico"));
        assertEquals(60, limiteDe("Enfermagem"));
        assertEquals(20, limiteDe("Odontologia"));
    }

    @Test
    void naoDuplicaSeRodarDuasVezes() {
        CargaServicosExemplo carga = new CargaServicosExemplo(servicoRepository, configuracaoServicoRepository);

        carga.run(null);
        carga.run(null);

        assertEquals(3, servicoRepository.count());
        assertEquals(3, configuracaoServicoRepository.count());
    }

    @Test
    void naoReativaServicoQueOGestorDesativou() {
        Servico odontologia = new Servico("Odontologia");
        odontologia.setAtivo(false);
        servicoRepository.save(odontologia);

        new CargaServicosExemplo(servicoRepository, configuracaoServicoRepository).run(null);

        assertEquals(3, servicoRepository.count());
        assertFalse(servicoRepository.findByNomeAndAtivoTrue("Odontologia").isPresent());
    }

    @Test
    void preservaConfiguracaoExistente() {
        Servico clinico = servicoRepository.save(new Servico("Clínico"));
        configuracaoServicoRepository.save(new ConfiguracaoServico(clinico,
            LocalTime.of(8, 0), LocalTime.of(16, 0), 9));

        new CargaServicosExemplo(servicoRepository, configuracaoServicoRepository).run(null);

        assertEquals(9, limiteDe("Clínico"));
        assertEquals(LocalTime.of(8, 0),
            configuracaoServicoRepository.findByServico_Id(clinico.getId()).orElseThrow().getHoraAbertura());
    }

    private int limiteDe(String nome) {
        Servico servico = servicoRepository.findByNome(nome).orElseThrow();
        return configuracaoServicoRepository.findByServico_Id(servico.getId()).orElseThrow().getLimiteSenhasDia();
    }
}
