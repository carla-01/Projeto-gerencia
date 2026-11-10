package br.uepb.filasus.fila;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicoService {

    private final ServicoRepository servicoRepository;
    private final ConfiguracaoServicoRepository configuracaoServicoRepository;

    public ServicoService(ServicoRepository servicoRepository,
                          ConfiguracaoServicoRepository configuracaoServicoRepository) {
        this.servicoRepository = servicoRepository;
        this.configuracaoServicoRepository = configuracaoServicoRepository;
    }

    @Transactional
    public ServicoResponse salvar(ServicoRequest request) {
        if (!request.horarioFechamento().isAfter(request.horarioAbertura())) {
            throw new HorarioInvalidoException();
        }

        String nome = request.nome().trim();
        if (servicoRepository.existsByNomeIgnoreCase(nome)) {
            throw new ServicoDuplicadoException();
        }

        Servico servico = servicoRepository.save(new Servico(nome));
        ConfiguracaoServico configuracao = configuracaoServicoRepository.save(
            new ConfiguracaoServico(servico, request.horarioAbertura(), request.horarioFechamento(),
                request.limiteSenhasDia()));
        return new ServicoResponse(servico, configuracao);
    }
}
