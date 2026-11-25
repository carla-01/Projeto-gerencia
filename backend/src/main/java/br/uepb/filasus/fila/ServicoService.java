package br.uepb.filasus.fila;

import java.util.List;

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

    /** Serviços ativos em ordem de nome: são os setores do cadastro de funcionários (US03). */
    @Transactional(readOnly = true)
    public List<ServicoResumoResponse> listarAtivos() {
        return servicoRepository.findByAtivoTrueOrderByNomeAsc().stream()
            .map(servico -> new ServicoResumoResponse(servico.getId(), servico.getNome()))
            .toList();
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
