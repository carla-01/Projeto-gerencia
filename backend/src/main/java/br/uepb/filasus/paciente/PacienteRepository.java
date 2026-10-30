package br.uepb.filasus.paciente;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso à tabela "paciente". A busca por CPF é sempre pelo hash,
 * porque o CPF em texto puro não fica no banco.
 */
public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    Optional<Paciente> findByCpfHash(String cpfHash);
}
