package br.uepb.filasus.fila;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.uepb.filasus.paciente.Paciente;

/** Acesso à tabela "senha". */
public interface SenhaRepository extends JpaRepository<Senha, Long> {

    /** Status de quem ainda espera ser chamado: conta na posição e pode fazer check-in ou mudar de prioridade. */
    List<StatusSenha> STATUS_NA_FILA = List.of(StatusSenha.AGUARDANDO, StatusSenha.PRESENTE);

    /**
     * Status em que a senha do paciente ainda está valendo: ele não pode tirar outra no mesmo
     * serviço. Inclui quem já foi chamado ou está em atendimento (US-13 - SCRUM-163).
     */
    List<StatusSenha> STATUS_ATIVOS = List.of(StatusSenha.AGUARDANDO, StatusSenha.PRESENTE,
        StatusSenha.CHAMADA, StatusSenha.EM_ATENDIMENTO);

    /**
     * Maior número de senha já gerado no serviço naquele dia, ou 0 se ainda não há nenhuma.
     * A próxima senha é esse valor + 1.
     */
    @Query("select coalesce(max(s.numero), 0) from Senha s where s.servico = :servico and s.dataRef = :dataRef")
    int maiorNumeroDoDia(@Param("servico") Servico servico, @Param("dataRef") LocalDate dataRef);

    long countByServicoAndDataRef(Servico servico, LocalDate dataRef);

    Optional<Senha> findFirstByPacienteAndServicoAndDataRefAndStatusIn(
        Paciente paciente, Servico servico, LocalDate dataRef, Collection<StatusSenha> status);

    /** Senha do paciente que ainda está valendo (STATUS_ATIVOS) no serviço, no dia. */
    default Optional<Senha> buscarSenhaAtiva(Paciente paciente, Servico servico, LocalDate dataRef) {
        return findFirstByPacienteAndServicoAndDataRefAndStatusIn(paciente, servico, dataRef, STATUS_ATIVOS);
    }

    List<Senha> findByPacienteAndDataRefAndStatusInOrderByNumeroAsc(
        Paciente paciente, LocalDate dataRef, Collection<StatusSenha> status);

    /** Senhas do paciente que ainda estão valendo no dia, uma por serviço (US-07). */
    default List<Senha> buscarSenhasAtivas(Paciente paciente, LocalDate dataRef) {
        return findByPacienteAndDataRefAndStatusInOrderByNumeroAsc(paciente, dataRef, STATUS_ATIVOS);
    }

    /** Senhas do paciente que ainda esperam ser chamadas no dia: usadas no check-in (US-10). */
    default List<Senha> buscarSenhasNaFila(Paciente paciente, LocalDate dataRef) {
        return findByPacienteAndDataRefAndStatusInOrderByNumeroAsc(paciente, dataRef, STATUS_NA_FILA);
    }

    List<Senha> findByServicoAndDataRefAndStatusIn(Servico servico, LocalDate dataRef, Collection<StatusSenha> status);

    /**
     * Chama a senha (US-13) só se ela ainda estiver PRESENTE. Devolve 1 quando chamou e 0 quando
     * outro profissional chegou antes, para duas pessoas não chamarem o mesmo paciente.
     */
    @Modifying(clearAutomatically = true)
    @Query("update Senha s set s.status = br.uepb.filasus.fila.StatusSenha.CHAMADA, s.chamadaEm = :em "
        + "where s.id = :id and s.status = br.uepb.filasus.fila.StatusSenha.PRESENTE")
    int chamarSePresente(@Param("id") Long id, @Param("em") Instant em);

    /**
     * Passa a senha para EM_ATENDIMENTO (US-14 - SCRUM-167) só se ela ainda estiver CHAMADA.
     * Devolve 1 quando mudou e 0 quando o atendimento já tinha sido iniciado, para dois cliques
     * juntos não abrirem dois atendimentos.
     */
    @Modifying(clearAutomatically = true)
    @Query("update Senha s set s.status = br.uepb.filasus.fila.StatusSenha.EM_ATENDIMENTO "
        + "where s.id = :id and s.status = br.uepb.filasus.fila.StatusSenha.CHAMADA")
    int iniciarAtendimentoSeChamada(@Param("id") Long id);

    /** Senhas com esse número no dia, uma por serviço: usada no check-in (US-10). */
    List<Senha> findByNumeroAndDataRefAndStatusInOrderByIdAsc(
        int numero, LocalDate dataRef, Collection<StatusSenha> status);

    /**
     * Senhas que ainda estão na fila do serviço no dia, na ordem de atendimento
     * com a prioridade legal (US-11 - SCRUM-159). A primeira é a próxima a ser chamada.
     */
    default List<Senha> filaDoDia(Servico servico, LocalDate dataRef) {
        return findByServicoAndDataRefAndStatusIn(servico, dataRef, STATUS_NA_FILA).stream()
            .sorted(OrdemFila.ORDEM)
            .toList();
    }
}
