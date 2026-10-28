package br.uepb.filasus.usuario;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso à tabela "usuarios". O Spring cria a implementação sozinho
 * a partir do nome dos métodos.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /** Usado no login por CPF. */
    Optional<Usuario> findByCpf(String cpf);

    /** Usado no login por e-mail. */
    Optional<Usuario> findByEmail(String email);
}
