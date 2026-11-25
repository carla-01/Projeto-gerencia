package br.uepb.filasus.fila;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.uepb.filasus.lgpd.Cpf;
import br.uepb.filasus.lgpd.ProtecaoCpf;
import br.uepb.filasus.paciente.Paciente;
import br.uepb.filasus.paciente.PacienteRepository;

/**
 * Check-in na recepção (US-10 - SCRUM-154).
 *
 * A recepção busca a senha de hoje pelo CPF ou pelo número e confirma a presença:
 * a senha muda de AGUARDANDO para PRESENTE e guarda o horário (checkin_em).
 */
@Service
public class CheckInService {

    static final String MENSAGEM_SEM_SENHA_HOJE = "Paciente sem senha hoje";
    static final String MENSAGEM_NUMERO_NAO_ENCONTRADO = "Nenhuma senha com esse número na fila de hoje";
    static final String MENSAGEM_BUSCA_INVALIDA = "Informe o CPF (11 números) ou o número da senha";
    static final String MENSAGEM_CPF_INVALIDO = "CPF inválido";
    static final String MENSAGEM_JA_CONFIRMADA = "A presença desta senha já foi confirmada";
    static final String MENSAGEM_FORA_DA_FILA = "Esta senha não está mais na fila";

    private static final int TAMANHO_CPF = 11;
    private static final int TAMANHO_MAXIMO_NUMERO = 4;

    private static final Logger log = LoggerFactory.getLogger(CheckInService.class);

    private final PacienteRepository pacienteRepository;
    private final SenhaRepository senhaRepository;
    private final ProtecaoCpf protecaoCpf;
    private final Clock clock;

    public CheckInService(PacienteRepository pacienteRepository, SenhaRepository senhaRepository,
                          ProtecaoCpf protecaoCpf, Clock clock) {
        this.pacienteRepository = pacienteRepository;
        this.senhaRepository = senhaRepository;
        this.protecaoCpf = protecaoCpf;
        this.clock = clock;
    }

    /** Procura as senhas de hoje pelo CPF ou pelo número da senha. */
    @Transactional(readOnly = true)
    public BuscaCheckInResponse buscar(String busca) {
        // Tira os espaços das pontas, os pontos e o traço do CPF.
        String texto = Cpf.normalizar(busca);

        if (!soTemNumeros(texto)) {
            throw new BuscaInvalidaException(MENSAGEM_BUSCA_INVALIDA);
        }
        if (texto.length() == TAMANHO_CPF) {
            return buscarPorCpf(texto);
        }
        if (texto.length() <= TAMANHO_MAXIMO_NUMERO) {
            return buscarPorNumero(Integer.parseInt(texto));
        }
        throw new BuscaInvalidaException(MENSAGEM_BUSCA_INVALIDA);
    }

    /** Confirma a presença do paciente (Cenário 1). */
    @Transactional
    public SenhaCheckInResponse confirmarPresenca(Long senhaId) {
        Senha senha = senhaRepository.findById(senhaId).orElseThrow(SenhaNaoEncontradaException::new);

        LocalDate hoje = LocalDate.now(clock);
        if (!senha.getDataRef().equals(hoje)) {
            throw new CheckInNaoPermitidoException(MENSAGEM_FORA_DA_FILA);
        }
        if (senha.getStatus() == StatusSenha.PRESENTE) {
            throw new CheckInNaoPermitidoException(MENSAGEM_JA_CONFIRMADA);
        }
        if (senha.getStatus() != StatusSenha.AGUARDANDO) {
            throw new CheckInNaoPermitidoException(MENSAGEM_FORA_DA_FILA);
        }

        senha.setStatus(StatusSenha.PRESENTE);
        senha.setCheckinEm(Instant.now(clock));
        senhaRepository.save(senha);

        log.info("Presença confirmada na senha id={}", senha.getId());
        return montarResposta(senha);
    }

    private BuscaCheckInResponse buscarPorCpf(String cpf) {
        if (!Cpf.ehValido(cpf)) {
            throw new BuscaInvalidaException(MENSAGEM_CPF_INVALIDO);
        }

        // No banco só fica o hash do CPF.
        Optional<Paciente> encontrado = pacienteRepository.findByCpfHash(protecaoCpf.hash(cpf));
        if (encontrado.isEmpty()) {
            return new BuscaCheckInResponse(MENSAGEM_SEM_SENHA_HOJE, null, null, new ArrayList<>());
        }

        Paciente paciente = encontrado.get();
        List<Senha> senhas = senhaRepository.buscarSenhasAtivas(paciente, LocalDate.now(clock));

        String mensagem = null;
        if (senhas.isEmpty()) {
            mensagem = MENSAGEM_SEM_SENHA_HOJE;
        }
        return new BuscaCheckInResponse(mensagem, paciente.getId(), paciente.getNome(), montarRespostas(senhas));
    }

    /** O número recomeça em cada serviço, então a busca pode achar mais de uma senha. */
    private BuscaCheckInResponse buscarPorNumero(int numero) {
        List<Senha> senhas = senhaRepository.findByNumeroAndDataRefAndStatusInOrderByIdAsc(
            numero, LocalDate.now(clock), SenhaRepository.STATUS_ATIVOS);

        String mensagem = null;
        if (senhas.isEmpty()) {
            mensagem = MENSAGEM_NUMERO_NAO_ENCONTRADO;
        }
        return new BuscaCheckInResponse(mensagem, null, null, montarRespostas(senhas));
    }

    private List<SenhaCheckInResponse> montarRespostas(List<Senha> senhas) {
        List<SenhaCheckInResponse> respostas = new ArrayList<>();
        for (Senha senha : senhas) {
            respostas.add(montarResposta(senha));
        }
        return respostas;
    }

    private SenhaCheckInResponse montarResposta(Senha senha) {
        String cpfMascarado = "";
        String cpfCifrado = senha.getPaciente().getCpfCifrado();
        if (cpfCifrado != null) {
            cpfMascarado = Cpf.mascarar(protecaoCpf.decifrar(cpfCifrado));
        }
        return new SenhaCheckInResponse(senha, cpfMascarado);
    }

    private boolean soTemNumeros(String texto) {
        if (texto.isEmpty()) {
            return false;
        }
        for (int i = 0; i < texto.length(); i++) {
            char letra = texto.charAt(i);
            if (letra < '0' || letra > '9') {
                return false;
            }
        }
        return true;
    }
}
