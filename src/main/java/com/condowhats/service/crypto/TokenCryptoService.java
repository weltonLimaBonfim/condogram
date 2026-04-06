package com.condowhats.service.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Criptografa/descriptografa o access token da Meta usando AES-256-GCM.
 * A chave fica em variável de ambiente — nunca no banco ou no código.
 * <p>
 * Formato do valor criptografado no banco: Base64(IV[12] + CipherText + Tag[16])
 */
@Service
public class TokenCryptoService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;   // 96 bits — recomendado para GCM
    private static final int TAG_LENGTH = 128;  // bits

    private final SecretKey secretKey;

    public TokenCryptoService(@Value("${condowhats.crypto.key}") String base64Key) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException("A chave AES deve ter exatamente 256 bits (32 bytes em Base64).");
        }
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Gera uma chave AES-256 aleatória em Base64 — use uma vez para gerar e salvar no .env
     */
    public static String generateKey() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }

    public String encrypt(String plainText) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH, iv));

            byte[] cipherText = cipher.doFinal(plainText.getBytes());
            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao criptografar token", e);
        }
    }

    public String decrypt(String encryptedBase64) {
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedBase64);
            byte[] iv = new byte[IV_LENGTH];
            byte[] cipher = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, cipher, 0, cipher.length);

            Cipher c = Cipher.getInstance(ALGORITHM);
            c.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH, iv));
            return new String(c.doFinal(cipher));
        } catch (Exception e) {
            throw new RuntimeException("Falha ao descriptografar token", e);
        }
    }
}
