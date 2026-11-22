package br.uepb.filasus.fila;

import java.time.Instant;
import java.time.LocalDate;

import br.uepb.filasus.paciente.Paciente;
import br.uepb.filasus.paciente.Prioridade;
import br.uepb.filasus.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Senha de um paciente na fila de um serviço (US-06 - SCRUM-41). Tabela "senha" do DER.
 *
 * O número recomeça a cada dia em cada serviço. A UNIQUE (servico_id, data_ref, numero)
 * garante que não saem duas senhas iguais mesmo com entradas ao mesmo tempo
 * (DA-DES-I2-01): a segunda gravação falha e a aplicação tenta com o próximo número.
 *
 * A recepção marca a prioridade legal (US-11 - SCRUM-158): a senha guarda quem marcou
 * (prioridade_por), quando (prioridade_em) e o motivo (prioridade_motivo).
 */
@Entity
@Table(name = "senha",
    uniqueConstraints = @UniqueConstraint(name = "uk_senha_servico_dia_numero",
        columnNames = {"servico_id", "data_ref", "numero"}),
    indexes = {
        @Index(name = "ix_senha_fila", columnList = "servico_id, data_ref, status"),
        @Index(name = "ix_senha_paciente", columnList = "paciente_id")
    })
public class Senha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "servico_id", nullable = false)
    private Servico servico;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    /** Número da senha no dia, começando em 1 em cada serviço. */
    @Column(nullable = false)
    private int numero;

    /** Dia da fila a que a senha pertence. */
    @Column(name = "data_ref", nullable = false)
    private LocalDate dataRef;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusSenha status = StatusSenha.AGUARDANDO;

    /** Prioridade legal usada na fila (os mesmos valores da prioridade do paciente). */
    @Enumerated(EnumType.STRING)
    @Column(name = "prioridade_aplicada", nullable = false, length = 20)
    private Prioridade prioridadeAplicada = Prioridade.NENHUMA;

    /** Recepcionista que marcou a prioridade (US-11). Nulo enquanto ninguém marcou. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prioridade_por")
    private Usuario prioridadePor;

    /** Quando a prioridade foi marcada (US-11). */
    @Column(name = "prioridade_em")
    private Instant prioridadeEm;

    /** Motivo informado pela recepção. Obrigatório na urgência (US-11, Cenário 2). */
    @Column(name = "prioridade_motivo", length = 255)
    private String prioridadeMotivo;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    /** Preenchido no check-in da recepção (US-10). */
    @Column(name = "checkin_em")
    private Instant checkinEm;

    /** Preenchido quando o profissional chama (US-13). */
    @Column(name = "chamada_em")
    private Instant chamadaEm;

    /** Construtor vazio exigido pelo JPA. */
    protected Senha() {
    }

    public Senha(Servico servico, Paciente paciente, int numero, LocalDate dataRef,
                 Prioridade prioridadeAplicada, Instant criadaEm) {
        this.servico = servico;
        this.paciente = paciente;
        this.numero = numero;
        this.dataRef = dataRef;
        this.prioridadeAplicada = prioridadeAplicada;
        this.criadaEm = criadaEm;
        this.status = StatusSenha.AGUARDANDO;
    }

    public Long getId() {
        return id;
    }

    public Servico getServico() {
        return servico;
    }

    public Paciente getPaciente() {
        return paciente;
    }

    public int getNumero() {
        return numero;
    }

    public LocalDate getDataRef() {
        return dataRef;
    }

    public StatusSenha getStatus() {
        return status;
    }

    public void setStatus(StatusSenha status) {
        this.status = status;
    }

    public Prioridade getPrioridadeAplicada() {
        return prioridadeAplicada;
    }

    public void setPrioridadeAplicada(Prioridade prioridadeAplicada) {
        this.prioridadeAplicada = prioridadeAplicada;
    }

    /** Marca a prioridade e registra quem marcou, quando e o motivo (US-11). */
    public void aplicarPrioridade(Prioridade prioridade, Usuario recepcionista, String motivo, Instant em) {
        this.prioridadeAplicada = prioridade;
        this.prioridadePor = recepcionista;
        this.prioridadeMotivo = motivo;
        this.prioridadeEm = em;
    }

    public Usuario getPrioridadePor() {
        return prioridadePor;
    }

    public Instant getPrioridadeEm() {
        return prioridadeEm;
    }

    public String getPrioridadeMotivo() {
        return prioridadeMotivo;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }

    public Instant getCheckinEm() {
        return checkinEm;
    }

    public void setCheckinEm(Instant checkinEm) {
        this.checkinEm = checkinEm;
    }

    public Instant getChamadaEm() {
        return chamadaEm;
    }

    public void setChamadaEm(Instant chamadaEm) {
        this.chamadaEm = chamadaEm;
    }
}
