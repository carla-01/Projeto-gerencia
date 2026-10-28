package br.uepb.filasus.usuario;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Usuário do sistema (US01 - SCRUM-37). Tabela "usuario" do DER.
 *
 * Nada de dado sensível em texto puro (DA-SEG-I3-01):
 * - a senha vai como hash bcrypt (PasswordEncoder do SecurityConfig);
 * - o CPF vai como cpfHash (para o login por CPF) e cpfCifrado (para exibir),
 *   os dois gerados pela ProtecaoCpf.
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String nome;

    @NotBlank
    @Email
    @Column(nullable = false, unique = true, length = 160)
    private String email;

    /** HMAC-SHA256 do CPF (64 caracteres). É por ele que o login acha o usuário. */
    @NotBlank
    @Column(name = "cpf_hash", nullable = false, unique = true, length = 64)
    private String cpfHash;

    /** CPF cifrado com AES-GCM. Só serve para mostrar o CPF na tela. */
    @NotBlank
    @Column(name = "cpf_cifrado", nullable = false, length = 255)
    private String cpfCifrado;

    /** Hash bcrypt da senha. */
    @NotBlank
    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash;

    /** Guardado como texto (ex.: "GESTOR") para o banco ficar fácil de ler. */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Perfil perfil;

    /** Usuário inativo não consegue fazer login. */
    @Column(nullable = false)
    private boolean ativo = true;

    /**
     * Senhas erradas seguidas desde o último login certo (DA-SEG-I2-01).
     * O "default 0" deixa o ddl-auto criar a coluna mesmo com usuários já salvos.
     */
    @Column(name = "tentativas_falhas", nullable = false, columnDefinition = "integer default 0")
    private int tentativasFalhas = 0;

    /** Até quando a conta está bloqueada. Fica vazio (null) se não estiver bloqueada. */
    @Column(name = "bloqueado_ate")
    private Instant bloqueadoAte;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    /** Construtor vazio exigido pelo JPA. */
    protected Usuario() {
    }

    public Usuario(String nome, String email, String cpfHash, String cpfCifrado, String senhaHash, Perfil perfil) {
        this.nome = nome;
        this.email = email;
        this.cpfHash = cpfHash;
        this.cpfCifrado = cpfCifrado;
        this.senhaHash = senhaHash;
        this.perfil = perfil;
        this.ativo = true;
    }

    /** Marca a data de criação na hora de salvar pela primeira vez. */
    @PrePersist
    void preencherCriadoEm() {
        if (criadoEm == null) {
            criadoEm = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCpfHash() {
        return cpfHash;
    }

    public String getCpfCifrado() {
        return cpfCifrado;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public void setSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public void setPerfil(Perfil perfil) {
        this.perfil = perfil;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public int getTentativasFalhas() {
        return tentativasFalhas;
    }

    public void setTentativasFalhas(int tentativasFalhas) {
        this.tentativasFalhas = tentativasFalhas;
    }

    public Instant getBloqueadoAte() {
        return bloqueadoAte;
    }

    public void setBloqueadoAte(Instant bloqueadoAte) {
        this.bloqueadoAte = bloqueadoAte;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
