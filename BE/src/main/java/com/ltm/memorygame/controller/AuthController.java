package com.ltm.memorygame.controller;

import com.ltm.memorygame.dto.auth.response.AuthResponse;
import com.ltm.memorygame.dto.auth.request.AuthRequest;
import com.ltm.memorygame.dto.auth.request.RegisterRequest;
import com.ltm.memorygame.dto.user.response.UserResponseDTO;
import com.ltm.memorygame.service.auth.AuthService;
import com.ltm.memorygame.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> register(@Valid @RequestBody RegisterRequest body) {
        return ResponseEntity.status(201).body(userService.createUser(body));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest body) {
        return ResponseEntity.ok(authService.login(body));
    }

    @PostMapping("/logout/{userId}")
    public ResponseEntity<Void> logout(@PathVariable Long userId) {
        authService.logout(userId);
        return ResponseEntity.noContent().build();
    }
}