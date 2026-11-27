package br.uepb.filasus.fila;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fila de exemplo para desenvolvimento (US-06 - SCRUM-41).
 *
 * Ao subir a aplicação, garante os mesmos serviços do docs/devops/banco/03-dados-iniciais.sql.
 * Só cria os que faltam, então pode rodar várias vezes sem duplicar, e não mexe em quem já
 * existe (se o gestor desativar um serviço, ele continua desativado).
 *
 * Não roda no perfil "test", para os testes começarem com o banco vazio.
 */
@Component
@Profile("!test")
public class CargaServicosExemplo implements ApplicationRunner {

    /** Serviços fictícios da UBS de exemplo. */
    static final List<String> SERVICOS = List.of("Clínico", "Enfermagem", "Odontologia");
    private static final Map<String, ConfiguracaoExemplo> CONFIGURACOES = Map.of(
        "Clínico", new ConfiguracaoExemplo(40),
        "Enfermagem", new ConfiguracaoExemplo(60),
        "Odontologia", new ConfiguracaoExemplo(20));

    private static final Logger log = LoggerFactory.getLogger(CargaServicosExemplo.class);

    private final ServicoRepository servicoRepository;
    private final ConfiguracaoServicoRepository configuracaoServicoRepository;

    public CargaServicosExemplo(ServicoRepository servicoRepository,
                                ConfiguracaoServicoRepository configuracaoServicoRepository) {
        this.servicoRepository = servicoRepository;
        this.configuracaoServicoRepository = configuracaoServicoRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int criados = 0;
        for (String nome : SERVICOS) {
            Servico servico = servicoRepository.findByNome(nome).orElse(null);
            if (servico == null) {
                servico = servicoRepository.save(new Servico(nome));
                criados++;
            }
            if (configuracaoServicoRepository.findByServico_Id(servico.getId()).isEmpty()) {
                ConfiguracaoExemplo configuracao = CONFIGURACOES.get(nome);
                configuracaoServicoRepository.save(new ConfiguracaoServico(servico,
                    configuracao.horaAbertura(), configuracao.horaFechamento(), configuracao.limite()));
            }
        }
        log.info("Fila de exemplo: {} serviço(s) criado(s), {} já existiam", criados, SERVICOS.size() - criados);
    }

    private record ConfiguracaoExemplo(LocalTime horaAbertura, LocalTime horaFechamento, int limite) {
        private ConfiguracaoExemplo(int limite) {
            this(LocalTime.of(7, 0), LocalTime.of(17, 0), limite);
        }
    }
}
