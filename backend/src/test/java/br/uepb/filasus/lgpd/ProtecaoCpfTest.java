package br.uepb.filasus.lgpd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** Proteção do CPF no banco (DA-SEG-I3-01 - SCRUM-39). */
class ProtecaoCpfTest {

    private static final String CPF = "52998224725";

    private final ProtecaoCpf protecao = new ProtecaoCpf("chave-hash-de-teste", "chave-cifra-de-teste");

    @Test
    void hashTem64CaracteresENaoMostraOCpf() {
        String hash = protecao.hash(CPF);

        assertEquals(64, hash.length());
        assertFalse(hash.contains(CPF));
    }

    @Test
    void mesmoCpfDaSempreOMesmoHash() {
        // É isso que permite buscar o paciente pelo CPF.
        assertEquals(protecao.hash(CPF), protecao.hash(CPF));
        assertNotEquals(protecao.hash(CPF), protecao.hash("11144477735"));
    }

    @Test
    void hashMudaComOutraChave() {
        // Sem a chave, não dá para descobrir o CPF testando todos os números.
        ProtecaoCpf outraChave = new ProtecaoCpf("outra-chave", "chave-cifra-de-teste");
        assertNotEquals(protecao.hash(CPF), outraChave.hash(CPF));
    }

    @Test
    void cifraEDecifra() {
        String cifrado = protecao.cifrar(CPF);

        assertFalse(cifrado.contains(CPF));
        assertEquals(CPF, protecao.decifrar(cifrado));
    }

    @Test
    void mesmoCpfCifradoDuasVezesFicaDiferente() {
        // Cada cifra usa um IV aleatório: olhando o banco, não dá para ver quem tem o mesmo CPF.
        assertNotEquals(protecao.cifrar(CPF), protecao.cifrar(CPF));
    }

    @Test
    void naoDecifraComOutraChave() {
        String cifrado = protecao.cifrar(CPF);
        ProtecaoCpf outraChave = new ProtecaoCpf("chave-hash-de-teste", "outra-chave");

        assertThrows(IllegalStateException.class, () -> outraChave.decifrar(cifrado));
    }
}
