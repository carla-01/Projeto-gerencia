package br.uepb.filasus.fila;

import java.util.List;

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
 * O horário e o limite de senhas de cada serviço entram com a US-09.
 */
@Component
@Profile("!test")
public class CargaServicosExemplo implements ApplicationRunner {

    /** Serviços fictícios da UBS de exemplo. */
    static final List<String> SERVICOS = List.of("Clínico", "Enfermagem", "Odontologia");

    private static final Logger log = LoggerFactory.getLogger(CargaServicosExemplo.class);

    private final ServicoRepository servicoRepository;

    public CargaServicosExemplo(ServicoRepository servicoRepository) {
        this.servicoRepository = servicoRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int criados = 0;
        for (String nome : SERVICOS) {
            if (!servicoRepository.existsByNome(nome)) {
                servicoRepository.save(new Servico(nome));
                criados++;
            }
        }
        log.info("Fila de exemplo: {} serviço(s) criado(s), {} já existiam", criados, SERVICOS.size() - criados);
    }
}
