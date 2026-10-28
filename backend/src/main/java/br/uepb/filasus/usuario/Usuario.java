package br.uepb.filasus.usuario;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Usuário do sistema (US01 - SCRUM-37).
 *
 * Vira a tabela "usuarios" no banco. A senha NUNCA é guardada em texto puro:
 * o campo senhaHash recebe o resultado do PasswordEncoder (bcrypt)
 * configurado em SecurityConfig.
 */
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** CPF só com os 11 números, sem ponto e sem traço. */
    @NotBlank
    @Size(min = 11, max = 11)
    @Column(nullable = false, unique = true, length = 11)
    private String cpf;

    @NotBlank
    @Email
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    /** Hash bcrypt da senha (sempre tem 60 caracteres). */
    @NotBlank
    @Column(name = "senha_hash", nullable = false, length = 60)
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

    /** Construtor vazio exigido pelo JPA. */
    protected Usuario() {
    }

    public Usuario(String cpf, String email, String senhaHash, Perfil perfil) {
        this.cpf = cpf;
        this.email = email;
        this.senhaHash = senhaHash;
        this.perfil = perfil;
        this.ativo = true;
    }

    public Long getId() {
        return id;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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
}
