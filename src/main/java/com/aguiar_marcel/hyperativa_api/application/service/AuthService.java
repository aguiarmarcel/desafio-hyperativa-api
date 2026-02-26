package com.aguiar_marcel.hyperativa_api.application.service;

import com.aguiar_marcel.hyperativa_api.adapters.in.web.auth.dto.AuthRequest;
import com.aguiar_marcel.hyperativa_api.adapters.in.web.auth.dto.AuthResponse;
import com.aguiar_marcel.hyperativa_api.adapters.out.persistence.UserRepository;
import com.aguiar_marcel.hyperativa_api.application.exception.InvalidCredentialsException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;

    @Value("${security.jwt.ttl-seconds}")
    private long ttlSeconds;

    public AuthResponse authenticate(AuthRequest request) {

        var user = userRepository.findByUsername(request.login())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        var now = Instant.now();
        var expiresAt = now.plusSeconds(ttlSeconds);

        var claims = JwtClaimsSet.builder()
                .subject(user.getUsername())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim("roles", List.of(user.getRole()))
                .build();

        var token = jwtEncoder
                .encode(JwtEncoderParameters.from(claims))
                .getTokenValue();

        return new AuthResponse(token, ttlSeconds);
    }
}