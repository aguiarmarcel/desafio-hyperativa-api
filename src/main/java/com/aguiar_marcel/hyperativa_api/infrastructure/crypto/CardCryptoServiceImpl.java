package com.aguiar_marcel.hyperativa_api.infrastructure.crypto;

import com.aguiar_marcel.hyperativa_api.application.port.CardCryptoService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class CardCryptoServiceImpl implements CardCryptoService {

    private static final String HMAC_ALG = "HmacSHA256";
    private static final String AES_ALG = "AES";
    private static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_BYTES = 12;

    @Value("${security.pan.hmac-secret}")
    private String hmacSecret;

    @Value("${security.pan.aes-key-b64}")
    private String aesKeyB64;

    private SecretKey hmacKey;
    private SecretKey aesKey;
    private final SecureRandom secureRandom = new SecureRandom();

    @PostConstruct
    void init() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(
                    hmacSecret.getBytes(StandardCharsets.UTF_8)
            );
            this.hmacKey = new SecretKeySpec(keyBytes, HMAC_ALG);

            byte[] aesKeyBytes = Base64.getDecoder().decode(aesKeyB64);
            this.aesKey = new SecretKeySpec(aesKeyBytes, AES_ALG);

        } catch (Exception e) {
            throw new IllegalStateException("Error initializing crypto keys", e);
        }
    }

    @Override
    public byte[] hmacSha256(String pan) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALG);
            mac.init(hmacKey);
            return mac.doFinal(pan.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Error computing HMAC", e);
        }
    }

    @Override
    public EncryptedPan encrypt(String pan) {
        try {
            byte[] iv = new byte[IV_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_BITS, iv));

            byte[] encrypted = cipher.doFinal(pan.getBytes(StandardCharsets.UTF_8));
            return new EncryptedPan(encrypted, iv);
        } catch (Exception e) {
            throw new IllegalStateException("Error encrypting PAN", e);
        }
    }
}
