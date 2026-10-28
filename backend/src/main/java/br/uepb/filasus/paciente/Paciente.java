package br.uepb.filasus.paciente;

import java.time.Instant;

import br.uepb.filasus.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Paciente da UBS (US-04 - SCRUM-39). Tabela "paciente" do DER.
 *
 * O CPF NUNCA é guardado em texto puro (DA-SEG-I3-01):
 * - cpfHash: usado para buscar o paciente pelo CPF;
 * - cpfCifrado: usado só para mostrar o CPF na tela.
 * Os dois são gerados pela ProtecaoCpf.
 *
 * No autocadastro (US-04), usuario é o login do próprio paciente.
 * Quem a recepção cadastra (US-05) não tem login: usuario fica nulo e
 * cadastradoPor guarda a recepcionista que fez o cadastro.
 */
@Entity
@Table(name = "paciente")
public class Paciente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Login do paciente. Nulo quando foi cadastrado pela recepção. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", unique = true)
    private Usuario usuario;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(name = "cpf_hash", unique = true, length = 64)
    private String cpfHash;

    @Column(name = "cpf_cifrado", length = 255)
    private String cpfCifrado;

    @Column(name = "cartao_sus", length = 15)
    private String cartaoSus;

    @Column(length = 20)
    private String telefone;

    @Column(length = 160)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Prioridade prioridade = Prioridade.NENHUMA;

    /** Recepcionista que cadastrou (US-05). Nulo no autocadastro (US-04). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cadastrado_por")
    private Usuario cadastradoPor;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    /** Preenchido quando o paciente pede a exclusão dos dados (US-21). */
    @Column(name = "anonimizado_em")
    private Instant anonimizadoEm;

    /** Construtor vazio exigido pelo JPA. */
    protected Paciente() {
    }

    public Paciente(String nome, String cpfHash, String cpfCifrado, Prioridade prioridade, Instant criadoEm) {
        this.nome = nome;
        this.cpfHash = cpfHash;
        this.cpfCifrado = cpfCifrado;
        this.prioridade = prioridade;
        this.criadoEm = criadoEm;
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCpfHash() {
        return cpfHash;
    }

    public String getCpfCifrado() {
        return cpfCifrado;
    }

    public String getCartaoSus() {
        return cartaoSus;
    }

    public void setCartaoSus(String cartaoSus) {
        this.cartaoSus = cartaoSus;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Prioridade getPrioridade() {
        return prioridade;
    }

    public void setPrioridade(Prioridade prioridade) {
        this.prioridade = prioridade;
    }

    public Usuario getCadastradoPor() {
        return cadastradoPor;
    }

    public void setCadastradoPor(Usuario cadastradoPor) {
        this.cadastradoPor = cadastradoPor;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAnonimizadoEm() {
        return anonimizadoEm;
    }
}
