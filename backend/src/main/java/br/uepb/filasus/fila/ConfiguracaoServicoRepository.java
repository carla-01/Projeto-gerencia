package br.uepb.filasus.fila;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracaoServicoRepository extends JpaRepository<ConfiguracaoServico, Long> {

    Optional<ConfiguracaoServico> findByServico_Id(Long servicoId);
}
