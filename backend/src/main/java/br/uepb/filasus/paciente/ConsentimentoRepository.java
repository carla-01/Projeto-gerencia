package br.uepb.filasus.paciente;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/** Acesso à tabela "consentimento". */
public interface ConsentimentoRepository extends JpaRepository<Consentimento, Long> {

    List<Consentimento> findByPacienteId(Long pacienteId);
}
