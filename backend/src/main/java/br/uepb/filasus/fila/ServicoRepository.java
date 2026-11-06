package br.uepb.filasus.fila;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ServicoRepository extends JpaRepository<Servico, Long> {
    boolean existsByNomeIgnoreCase(String nome);
}
