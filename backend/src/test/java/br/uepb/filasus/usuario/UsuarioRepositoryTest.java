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

import br.uepb.filasus.lgpd.ProtecaoCpf;

@SpringBootTest
@ActiveProfiles("test")
class UsuarioRepositoryTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ProtecaoCpf protecaoCpf;

    /** Cria o usuário com o CPF já protegido, como a aplicação faz. */
    private Usuario novoUsuario(String cpf, String email, String senhaHash, Perfil perfil) {
        return new Usuario("Usuário de Teste", email, protecaoCpf.hash(cpf), protecaoCpf.cifrar(cpf), senhaHash, perfil);
    }

    @BeforeEach
    void limparTabela() {
        usuarioRepository.deleteAll();
    }

    @Test
    void salvaUsuarioComSenhaEmBcrypt() {
        String senhaHash = passwordEncoder.encode("senha123");
        Usuario usuario = novoUsuario("12345678901", "maria@email.com", senhaHash, Perfil.PACIENTE);

        usuarioRepository.save(usuario);

        Optional<Usuario> encontrado = usuarioRepository.findByCpfHash(protecaoCpf.hash("12345678901"));
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

        // O CPF também não fica em texto puro (DA-SEG-I3-01), mas dá para exibir decifrando.
        assertNotEquals("12345678901", salvo.getCpfHash());
        assertFalse(salvo.getCpfCifrado().contains("12345678901"));
        assertEquals("12345678901", protecaoCpf.decifrar(salvo.getCpfCifrado()));
        assertNotNull(salvo.getCriadoEm());
    }

    @Test
    void encontraUsuarioPorEmail() {
        String senhaHash = passwordEncoder.encode("senha123");
        usuarioRepository.save(novoUsuario("12345678901", "gestor@ubs.com", senhaHash, Perfil.GESTOR));

        Optional<Usuario> encontrado = usuarioRepository.findByEmail("gestor@ubs.com");

        assertTrue(encontrado.isPresent());
        assertEquals(Perfil.GESTOR, encontrado.get().getPerfil());
    }

    @Test
    void naoEncontraCpfInexistente() {
        Optional<Usuario> encontrado = usuarioRepository.findByCpfHash(protecaoCpf.hash("00000000000"));

        assertFalse(encontrado.isPresent());
    }

    @Test
    void naoPermiteCpfRepetido() {
        String senhaHash = passwordEncoder.encode("senha123");
        usuarioRepository.save(novoUsuario("12345678901", "um@email.com", senhaHash, Perfil.PACIENTE));

        Usuario repetido = novoUsuario("12345678901", "outro@email.com", senhaHash, Perfil.RECEPCIONISTA);

        assertThrows(DataIntegrityViolationException.class, () -> usuarioRepository.saveAndFlush(repetido));
    }

    @Test
    void naoPermiteEmailRepetido() {
        String senhaHash = passwordEncoder.encode("senha123");
        usuarioRepository.save(novoUsuario("12345678901", "mesmo@email.com", senhaHash, Perfil.PACIENTE));

        Usuario repetido = novoUsuario("10987654321", "mesmo@email.com", senhaHash, Perfil.PROFISSIONAL);

        assertThrows(DataIntegrityViolationException.class, () -> usuarioRepository.saveAndFlush(repetido));
    }
}
