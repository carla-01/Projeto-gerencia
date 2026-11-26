package br.uepb.filasus.atendimento;

import java.time.Instant;

import br.uepb.filasus.fila.Senha;
import br.uepb.filasus.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Atendimento de uma senha chamada (US-14 - SCRUM-167). Tabela "atendimento" do DER.
 *
 * Guarda quem atendeu e os horários de início (iniciado_em) e fim (finalizado_em),
 * usados para medir o tempo de atendimento (US-16 e US-18). A UNIQUE (senha_id)
 * impede abrir dois atendimentos para a mesma senha.
 */
@Entity
@Table(name = "atendimento",
    uniqueConstraints = @UniqueConstraint(name = "uk_atendimento_senha", columnNames = "senha_id"),
    indexes = @Index(name = "ix_atendimento_profissional", columnList = "profissional_id"))
public class Atendimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "senha_id", nullable = false)
    private Senha senha;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profissional_id", nullable = false)
    private Usuario profissional;

    @Column(name = "iniciado_em", nullable = false)
    private Instant iniciadoEm;

    /** Nulo enquanto o atendimento não termina. */
    @Column(name = "finalizado_em")
    private Instant finalizadoEm;

    /** Observação curta do profissional (US-15). */
    @Column(length = 255)
    private String observacao;

    /** Construtor vazio exigido pelo JPA. */
    protected Atendimento() {
    }

    public Atendimento(Senha senha, Usuario profissional, Instant iniciadoEm) {
        this.senha = senha;
        this.profissional = profissional;
        this.iniciadoEm = iniciadoEm;
    }

    public Long getId() {
        return id;
    }

    public Senha getSenha() {
        return senha;
    }

    public Usuario getProfissional() {
        return profissional;
    }

    public Instant getIniciadoEm() {
        return iniciadoEm;
    }

    public Instant getFinalizadoEm() {
        return finalizadoEm;
    }

    public void setFinalizadoEm(Instant finalizadoEm) {
        this.finalizadoEm = finalizadoEm;
    }

    public String getObservacao() {
        return observacao;
    }
}
