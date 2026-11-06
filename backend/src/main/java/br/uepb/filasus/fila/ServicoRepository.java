package br.uepb.filasus.fila;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** Acesso à tabela "servico". */
public interface ServicoRepository extends JpaRepository<Servico, Long> {

    /** Usado no cadastro do gestor (US-09): o nome não pode repetir, nem com outra caixa. */
    boolean existsByNomeIgnoreCase(String nome);

    /** Usado na entrada na fila (US-06): só serviço ativo recebe senha. */
    Optional<Servico> findByNomeAndAtivoTrue(String nome);

    /** Usado pela carga da fila de exemplo para não duplicar serviços. */
    boolean existsByNome(String nome);
}
