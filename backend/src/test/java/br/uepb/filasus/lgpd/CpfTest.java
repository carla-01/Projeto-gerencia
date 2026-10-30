package br.uepb.filasus.lgpd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Validação do CPF (US-04 - SCRUM-39). Só com CPFs fictícios. */
class CpfTest {

    @Test
    void tiraPontosTracoEEspacos() {
        assertEquals("52998224725", Cpf.normalizar(" 529.982.247-25 "));
        assertEquals("", Cpf.normalizar(null));
    }

    @Test
    void aceitaCpfComDigitosCertos() {
        assertTrue(Cpf.ehValido("52998224725"));
        assertTrue(Cpf.ehValido("11144477735"));
    }

    @Test
    void recusaDigitoVerificadorErrado() {
        assertFalse(Cpf.ehValido("52998224700"));
        assertFalse(Cpf.ehValido("52998224726"));
    }

    @Test
    void recusaTamanhoErradoOuLetras() {
        assertFalse(Cpf.ehValido("5299822472"));
        assertFalse(Cpf.ehValido("529982247250"));
        assertFalse(Cpf.ehValido("5299822472a"));
        assertFalse(Cpf.ehValido(null));
    }

    @Test
    void recusaTodosOsNumerosIguais() {
        // 111.111.111-11 passa na conta dos dígitos, mas não é um CPF de verdade.
        assertFalse(Cpf.ehValido("11111111111"));
        assertFalse(Cpf.ehValido("00000000000"));
    }

    @Test
    void mascaraOMeioDoCpf() {
        assertEquals("529.***.***-25", Cpf.mascarar("52998224725"));
        assertEquals("", Cpf.mascarar("123"));
        assertEquals("", Cpf.mascarar(null));
    }
}
