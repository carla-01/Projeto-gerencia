package br.uepb.filasus.atendimento;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** Acesso à tabela "atendimento". */
public interface AtendimentoRepository extends JpaRepository<Atendimento, Long> {

    Optional<Atendimento> findBySenha_Id(Long senhaId);
}
