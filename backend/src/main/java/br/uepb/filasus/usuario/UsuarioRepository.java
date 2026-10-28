package br.uepb.filasus.usuario;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso à tabela "usuario". O Spring cria a implementação sozinho
 * a partir do nome dos métodos.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /** Usado no login por CPF: recebe o hash do CPF (ProtecaoCpf.hash), nunca o CPF. */
    Optional<Usuario> findByCpfHash(String cpfHash);

    /** Usado no login por e-mail. */
    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);
}
