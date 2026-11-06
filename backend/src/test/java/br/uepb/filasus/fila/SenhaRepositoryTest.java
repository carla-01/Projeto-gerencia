package br.uepb.filasus.fila;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import br.uepb.filasus.lgpd.ProtecaoCpf;
import br.uepb.filasus.paciente.Paciente;
import br.uepb.filasus.paciente.PacienteRepository;
import br.uepb.filasus.paciente.Prioridade;

/** Entidades Servico e Senha (US-06 - SCRUM-41). */
@SpringBootTest
@ActiveProfiles("test")
class SenhaRepositoryTest {

    /** CPF fictício, válido só no formato. */
    private static final String CPF = "11144477735";
    private static final LocalDate HOJE = LocalDate.of(2026, 11, 5);

    @Autowired
    private SenhaRepository senhaRepository;

    @Autowired
    private ServicoRepository servicoRepository;

    @Autowired
    private ConfiguracaoServicoRepository configuracaoServicoRepository;

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private ProtecaoCpf protecaoCpf;

    private Servico clinico;
    private Paciente paciente;

    @BeforeEach
    void criarServicoEPaciente() {
        senhaRepository.deleteAll();
        configuracaoServicoRepository.deleteAll();
        servicoRepository.deleteAll();
        clinico = servicoRepository.save(new Servico("Clínico"));
        paciente = pacienteRepository.save(new Paciente("Paciente Fictício", protecaoCpf.hash(CPF),
            protecaoCpf.cifrar(CPF), Prioridade.NENHUMA, Instant.now()));
    }

    /** Limpa o que o teste criou, para não travar a limpeza de pacientes dos outros testes. */
    @AfterEach
    void limparTabelas() {
        senhaRepository.deleteAll();
        servicoRepository.deleteAll();
        pacienteRepository.delete(paciente);
    }

    private Senha novaSenha(int numero, LocalDate dia) {
        return new Senha(clinico, paciente, numero, dia, Prioridade.NENHUMA, Instant.now());
    }

    @Test
    void salvaServicoESenha() {
        Senha salva = senhaRepository.save(novaSenha(1, HOJE));

        Senha encontrada = senhaRepository.findById(salva.getId()).orElseThrow();
        assertNotNull(encontrada.getId());
        assertEquals(clinico.getId(), encontrada.getServico().getId());
        assertEquals(paciente.getId(), encontrada.getPaciente().getId());
        assertEquals(1, encontrada.getNumero());
        assertEquals(HOJE, encontrada.getDataRef());
        assertEquals(StatusSenha.AGUARDANDO, encontrada.getStatus());
        assertEquals(Prioridade.NENHUMA, encontrada.getPrioridadeAplicada());
        assertTrue(servicoRepository.findByNomeAndAtivoTrue("Clínico").isPresent());
    }

    @Test
    void naoAceitaDuasSenhasComOMesmoServicoDiaENumero() {
        senhaRepository.saveAndFlush(novaSenha(1, HOJE));

        assertThrows(DataIntegrityViolationException.class,
            () -> senhaRepository.saveAndFlush(novaSenha(1, HOJE)));
    }

    @Test
    void aceitaOMesmoNumeroEmOutroDia() {
        senhaRepository.saveAndFlush(novaSenha(1, HOJE));
        senhaRepository.saveAndFlush(novaSenha(1, HOJE.plusDays(1)));

        assertEquals(2, senhaRepository.count());
    }

    @Test
    void maiorNumeroDoDiaComecaEmZero() {
        assertEquals(0, senhaRepository.maiorNumeroDoDia(clinico, HOJE));

        senhaRepository.save(novaSenha(1, HOJE));
        senhaRepository.save(novaSenha(2, HOJE));
        senhaRepository.save(novaSenha(7, HOJE.plusDays(1)));

        assertEquals(2, senhaRepository.maiorNumeroDoDia(clinico, HOJE));
    }

    @Test
    void encontraSoASenhaAtivaDoPacienteNoDia() {
        Senha cancelada = novaSenha(1, HOJE);
        cancelada.setStatus(StatusSenha.CANCELADA);
        senhaRepository.save(cancelada);

        assertFalse(senhaRepository.buscarSenhaAtiva(paciente, clinico, HOJE).isPresent());

        senhaRepository.save(novaSenha(2, HOJE));

        Optional<Senha> ativa = senhaRepository.buscarSenhaAtiva(paciente, clinico, HOJE);
        assertTrue(ativa.isPresent());
        assertEquals(2, ativa.get().getNumero());
    }

    @Test
    void naoEncontraServicoInativo() {
        clinico.setAtivo(false);
        servicoRepository.save(clinico);

        assertFalse(servicoRepository.findByNomeAndAtivoTrue("Clínico").isPresent());
    }
}
