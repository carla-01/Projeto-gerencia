package br.uepb.filasus.lgpd;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Protege o CPF antes de ir para o banco (DA-SEG-I3-01: 0 CPF em texto claro).
 *
 * - hash(): HMAC-SHA256 com chave secreta, em hexadecimal (64 caracteres).
 *   Vai na coluna cpf_hash e serve para buscar pelo CPF. Usamos HMAC em vez de
 *   um SHA-256 simples porque só existem 1 bilhão de CPFs: sem a chave, daria
 *   para descobrir o CPF testando todos.
 * - cifrar()/decifrar(): AES-256-GCM. Vai na coluna cpf_cifrado e serve só
 *   para mostrar o CPF na tela.
 *
 * As chaves vêm de variáveis de ambiente (CPF_CHAVE_HASH e CPF_CHAVE_CIFRA).
 * Se a chave de cifra mudar, os CPFs já cifrados não abrem mais.
 */
@Service
public class ProtecaoCpf {

    private static final int TAMANHO_IV = 12;
    private static final int TAMANHO_TAG_BITS = 128;

    private final SecretKeySpec chaveHash;
    private final SecretKeySpec chaveCifra;
    private final SecureRandom aleatorio = new SecureRandom();

    public ProtecaoCpf(@Value("${cpf.chave-hash}") String segredoHash,
                       @Value("${cpf.chave-cifra}") String segredoCifra) {
        this.chaveHash = new SecretKeySpec(segredoHash.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        // O AES-256 precisa de uma chave de exatamente 32 bytes: o SHA-256 do segredo tem esse tamanho.
        this.chaveCifra = new SecretKeySpec(sha256(segredoCifra), "AES");
    }

    /** Hash do CPF já normalizado, para a coluna cpf_hash. */
    public String hash(String cpf) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(chaveHash);
            byte[] resultado = mac.doFinal(cpf.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resultado);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Não foi possível gerar o hash do CPF", e);
        }
    }

    /** CPF cifrado em Base64 (IV + texto cifrado), para a coluna cpf_cifrado. */
    public String cifrar(String cpf) {
        try {
            byte[] iv = new byte[TAMANHO_IV];
            aleatorio.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, chaveCifra, new GCMParameterSpec(TAMANHO_TAG_BITS, iv));
            byte[] cifrado = cipher.doFinal(cpf.getBytes(StandardCharsets.UTF_8));

            byte[] ivMaisCifrado = ByteBuffer.allocate(iv.length + cifrado.length).put(iv).put(cifrado).array();
            return Base64.getEncoder().encodeToString(ivMaisCifrado);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Não foi possível cifrar o CPF", e);
        }
    }

    /** Volta o valor da coluna cpf_cifrado para o CPF, só para exibir. */
    public String decifrar(String cpfCifrado) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(Base64.getDecoder().decode(cpfCifrado));
            byte[] iv = new byte[TAMANHO_IV];
            buffer.get(iv);
            byte[] cifrado = new byte[buffer.remaining()];
            buffer.get(cifrado);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, chaveCifra, new GCMParameterSpec(TAMANHO_TAG_BITS, iv));
            return new String(cipher.doFinal(cifrado), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Não foi possível decifrar o CPF", e);
        }
    }

    private static byte[] sha256(String texto) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
