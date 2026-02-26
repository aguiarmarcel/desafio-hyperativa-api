package com.aguiar_marcel.hyperativa_api.application.port;

public interface CardCryptoService {
    byte[] hmacSha256(String pan);

    EncryptedPan encrypt(String pan);

    record EncryptedPan(byte[] cipherText, byte[] iv) {}
}
