package com.aguiar_marcel.hyperativa_api.adapters.in.web;

import com.aguiar_marcel.hyperativa_api.adapters.in.web.auth.dto.AuthRequest;
import com.aguiar_marcel.hyperativa_api.adapters.in.web.auth.dto.AuthResponse;
import com.aguiar_marcel.hyperativa_api.application.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody AuthRequest request
    ) {

        AuthResponse response = authService.authenticate(request);

        return ResponseEntity.ok(response);
    }
}