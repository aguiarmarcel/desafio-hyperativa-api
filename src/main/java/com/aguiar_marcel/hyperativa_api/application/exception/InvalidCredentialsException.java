package com.aguiar_marcel.hyperativa_api.application.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.net.URI;

public class InvalidCredentialsException extends RuntimeException{

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ProblemDetail> invalidCredentials(InvalidCredentialsException ex, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        pd.setTitle("Unauthorized");
        pd.setDetail("Invalid credentials");
        pd.setInstance(URI.create(req.getRequestURI()));
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(pd);
    }
}
