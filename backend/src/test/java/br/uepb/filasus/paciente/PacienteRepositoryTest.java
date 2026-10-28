package br.uepb.filasus.paciente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import br.uepb.filasus.lgpd.ProtecaoCpf;

/** Entidade Paciente (US-04 - SCRUM-39). */
@SpringBootTest
@ActiveProfiles("test")
class PacienteRepositoryTest {

    private static final String CPF = "52998224725";

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private ProtecaoCpf protecaoCpf;

    @BeforeEach
    void limparTabela() {
        pacienteRepository.deleteAll();
    }

    private Paciente novoPaciente(String cpf) {
        return new Paciente("Maria da Silva", protecaoCpf.hash(cpf), protecaoCpf.cifrar(cpf),
            Prioridade.GESTANTE, Instant.now());
    }

    @Test
    void salvaEEncontraPeloHashDoCpf() {
        Paciente paciente = novoPaciente(CPF);
        paciente.setCartaoSus("898001234567890");
        pacienteRepository.save(paciente);

        Optional<Paciente> encontrado = pacienteRepository.findByCpfHash(protecaoCpf.hash(CPF));

        assertTrue(encontrado.isPresent());
        Paciente salvo = encontrado.get();
        assertNotNull(salvo.getId());
        assertEquals("Maria da Silva", salvo.getNome());
        assertEquals(Prioridade.GESTANTE, salvo.getPrioridade());
        assertEquals("898001234567890", salvo.getCartaoSus());
        assertNull(salvo.getAnonimizadoEm());
        // O CPF só volta decifrando: no banco ficam o hash e o valor cifrado.
        assertEquals(CPF, protecaoCpf.decifrar(salvo.getCpfCifrado()));
    }

    @Test
    void naoAceitaDoisPacientesComOMesmoCpf() {
        pacienteRepository.save(novoPaciente(CPF));

        assertThrows(DataIntegrityViolationException.class,
            () -> pacienteRepository.saveAndFlush(novoPaciente(CPF)));
    }
}
