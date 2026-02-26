package com.aguiar_marcel.hyperativa_api.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.nio.file.Path;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Configuration
public class JwtKeysConfig {

    @Bean
    JwtDecoder jwtDecoder(@Value("${security.jwt.public-key-path}") String publicKeyPath) throws Exception {
        RSAPublicKey publicKey = PemUtils.readPublicKey(Path.of(publicKeyPath));
        return NimbusJwtDecoder.withPublicKey(publicKey).build();
    }

    @Bean
    JwtEncoder jwtEncoder(
            @Value("${security.jwt.public-key-path}") String publicKeyPath,
            @Value("${security.jwt.private-key-path}") String privateKeyPath
    ) throws Exception {

        RSAPublicKey publicKey = PemUtils.readPublicKey(Path.of(publicKeyPath));
        RSAPrivateKey privateKey = PemUtils.readPrivateKey(Path.of(privateKeyPath));

        var jwk = new com.nimbusds.jose.jwk.RSAKey.Builder(publicKey).privateKey(privateKey).build();
        var jwkSource = new com.nimbusds.jose.jwk.source.ImmutableJWKSet<>(new com.nimbusds.jose.jwk.JWKSet(jwk));
        return new NimbusJwtEncoder(jwkSource);
    }
}
