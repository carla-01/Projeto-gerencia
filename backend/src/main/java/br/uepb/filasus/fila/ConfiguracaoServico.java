package br.uepb.filasus.fila;

import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "configuracao_servico")
public class ConfiguracaoServico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "servico_id", nullable = false, unique = true)
    private Servico servico;

    @Column(name = "hora_abertura", nullable = false)
    private LocalTime horaAbertura;

    @Column(name = "hora_fechamento", nullable = false)
    private LocalTime horaFechamento;

    @Column(name = "limite_senhas_dia", nullable = false)
    private int limiteSenhasDia = 1;

    protected ConfiguracaoServico() {
    }

    public ConfiguracaoServico(Servico servico, LocalTime horaAbertura, LocalTime horaFechamento) {
        this.servico = servico;
        this.horaAbertura = horaAbertura;
        this.horaFechamento = horaFechamento;
    }

    public Long getId() {
        return id;
    }

    public Servico getServico() {
        return servico;
    }

    public LocalTime getHoraAbertura() {
        return horaAbertura;
    }

    public LocalTime getHoraFechamento() {
        return horaFechamento;
    }
}
