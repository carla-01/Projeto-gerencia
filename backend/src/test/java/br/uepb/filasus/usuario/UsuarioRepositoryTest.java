package br.uepb.filasus.usuario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class UsuarioRepositoryTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void limparTabela() {
        usuarioRepository.deleteAll();
    }

    @Test
    void salvaUsuarioComSenhaEmBcrypt() {
        String senhaHash = passwordEncoder.encode("senha123");
        Usuario usuario = new Usuario("12345678901", "maria@email.com", senhaHash, Perfil.PACIENTE);

        usuarioRepository.save(usuario);

        Optional<Usuario> encontrado = usuarioRepository.findByCpf("12345678901");
        assertTrue(encontrado.isPresent());

        Usuario salvo = encontrado.get();
        assertNotNull(salvo.getId());
        assertEquals("maria@email.com", salvo.getEmail());
        assertEquals(Perfil.PACIENTE, salvo.getPerfil());
        assertTrue(salvo.isAtivo());

        // A senha não pode estar em texto puro no banco...
        assertNotEquals("senha123", salvo.getSenhaHash());
        // ...e o hash bcrypt começa com "$2" e tem 60 caracteres.
        assertTrue(salvo.getSenhaHash().startsWith("$2"));
        assertEquals(60, salvo.getSenhaHash().length());
        // O hash confere com a senha original.
        assertTrue(passwordEncoder.matches("senha123", salvo.getSenhaHash()));
    }

    @Test
    void encontraUsuarioPorEmail() {
        String senhaHash = passwordEncoder.encode("senha123");
        usuarioRepository.save(new Usuario("12345678901", "gestor@ubs.com", senhaHash, Perfil.GESTOR));

        Optional<Usuario> encontrado = usuarioRepository.findByEmail("gestor@ubs.com");

        assertTrue(encontrado.isPresent());
        assertEquals(Perfil.GESTOR, encontrado.get().getPerfil());
    }

    @Test
    void naoEncontraCpfInexistente() {
        Optional<Usuario> encontrado = usuarioRepository.findByCpf("00000000000");

        assertFalse(encontrado.isPresent());
    }

    @Test
    void naoPermiteCpfRepetido() {
        String senhaHash = passwordEncoder.encode("senha123");
        usuarioRepository.save(new Usuario("12345678901", "um@email.com", senhaHash, Perfil.PACIENTE));

        Usuario repetido = new Usuario("12345678901", "outro@email.com", senhaHash, Perfil.RECEPCIONISTA);

        assertThrows(DataIntegrityViolationException.class, () -> usuarioRepository.saveAndFlush(repetido));
    }

    @Test
    void naoPermiteEmailRepetido() {
        String senhaHash = passwordEncoder.encode("senha123");
        usuarioRepository.save(new Usuario("12345678901", "mesmo@email.com", senhaHash, Perfil.PACIENTE));

        Usuario repetido = new Usuario("10987654321", "mesmo@email.com", senhaHash, Perfil.PROFISSIONAL);

        assertThrows(DataIntegrityViolationException.class, () -> usuarioRepository.saveAndFlush(repetido));
    }
}
