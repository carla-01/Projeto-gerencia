package br.uepb.filasus.fila;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Serviço da UBS (clínico, enfermagem, odontologia...). Tabela "servico" do DER.
 *
 * Cada serviço tem a sua fila do dia (US-06 - SCRUM-41).
 * O horário de funcionamento e o limite de senhas por dia ficam na
 * ConfiguracaoServico, cadastrada pelo gestor na US-09 (configurar filas e horários).
 */
@Entity
@Table(name = "servico")
public class Servico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String nome;

    /** Serviço inativo não aparece para o paciente entrar na fila. */
    @Column(nullable = false)
    private boolean ativo = true;

    /** Construtor vazio exigido pelo JPA. */
    protected Servico() {
    }

    public Servico(String nome) {
        this.nome = nome;
        this.ativo = true;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}
