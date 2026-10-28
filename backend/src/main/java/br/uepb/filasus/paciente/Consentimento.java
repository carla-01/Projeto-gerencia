package br.uepb.filasus.paciente;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Aceite do termo LGPD (US-04 - SCRUM-39). Tabela "consentimento" do DER.
 *
 * Guarda a versão do termo e a data e hora do aceite (CT-US04-01).
 * Cada aceite é uma linha nova: se o termo mudar de versão, o histórico fica.
 */
@Entity
@Table(name = "consentimento")
public class Consentimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @Column(name = "versao_termo", nullable = false, length = 20)
    private String versaoTermo;

    @Column(nullable = false)
    private boolean aceito;

    @Column(name = "aceito_em", nullable = false)
    private Instant aceitoEm;

    /** Construtor vazio exigido pelo JPA. */
    protected Consentimento() {
    }

    public Consentimento(Paciente paciente, String versaoTermo, boolean aceito, Instant aceitoEm) {
        this.paciente = paciente;
        this.versaoTermo = versaoTermo;
        this.aceito = aceito;
        this.aceitoEm = aceitoEm;
    }

    public Long getId() {
        return id;
    }

    public Paciente getPaciente() {
        return paciente;
    }

    public String getVersaoTermo() {
        return versaoTermo;
    }

    public boolean isAceito() {
        return aceito;
    }

    public Instant getAceitoEm() {
        return aceitoEm;
    }
}
