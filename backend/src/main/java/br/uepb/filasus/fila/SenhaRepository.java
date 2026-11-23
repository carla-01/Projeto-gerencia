package br.uepb.filasus.fila;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.uepb.filasus.paciente.Paciente;

/** Acesso à tabela "senha". */
public interface SenhaRepository extends JpaRepository<Senha, Long> {

    /** Status em que a senha ainda está valendo na fila (o paciente não pode tirar outra). */
    List<StatusSenha> STATUS_ATIVOS = List.of(StatusSenha.AGUARDANDO, StatusSenha.PRESENTE);

    /**
     * Maior número de senha já gerado no serviço naquele dia, ou 0 se ainda não há nenhuma.
     * A próxima senha é esse valor + 1.
     */
    @Query("select coalesce(max(s.numero), 0) from Senha s where s.servico = :servico and s.dataRef = :dataRef")
    int maiorNumeroDoDia(@Param("servico") Servico servico, @Param("dataRef") LocalDate dataRef);

    long countByServicoAndDataRef(Servico servico, LocalDate dataRef);

    Optional<Senha> findFirstByPacienteAndServicoAndDataRefAndStatusIn(
        Paciente paciente, Servico servico, LocalDate dataRef, Collection<StatusSenha> status);

    /** Senha do paciente que ainda está valendo (AGUARDANDO ou PRESENTE) no serviço, no dia. */
    default Optional<Senha> buscarSenhaAtiva(Paciente paciente, Servico servico, LocalDate dataRef) {
        return findFirstByPacienteAndServicoAndDataRefAndStatusIn(paciente, servico, dataRef, STATUS_ATIVOS);
    }

    List<Senha> findByPacienteAndDataRefAndStatusInOrderByNumeroAsc(
        Paciente paciente, LocalDate dataRef, Collection<StatusSenha> status);

    /** Senhas do paciente que ainda estão valendo no dia, uma por serviço (US-07). */
    default List<Senha> buscarSenhasAtivas(Paciente paciente, LocalDate dataRef) {
        return findByPacienteAndDataRefAndStatusInOrderByNumeroAsc(paciente, dataRef, STATUS_ATIVOS);
    }

    List<Senha> findByServicoAndDataRefAndStatusIn(Servico servico, LocalDate dataRef, Collection<StatusSenha> status);

    /**
     * Senhas que ainda estão na fila do serviço no dia, na ordem de atendimento
     * com a prioridade legal (US-11 - SCRUM-159). A primeira é a próxima a ser chamada.
     */
    default List<Senha> filaDoDia(Servico servico, LocalDate dataRef) {
        return findByServicoAndDataRefAndStatusIn(servico, dataRef, STATUS_ATIVOS).stream()
            .sorted(OrdemFila.ORDEM)
            .toList();
    }
}
