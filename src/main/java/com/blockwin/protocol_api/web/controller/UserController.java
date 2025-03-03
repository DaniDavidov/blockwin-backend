package com.blockwin.protocol_api.web.controller;

import com.blockwin.protocol_api.model.dto.LoginRequest;
import com.blockwin.protocol_api.model.dto.RegisterRequest;
import com.blockwin.protocol_api.model.dto.UserDTO;
import com.blockwin.protocol_api.model.entity.UserEntity;
import com.blockwin.protocol_api.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/api/v1/auth/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(this.userService.login(loginRequest));
    }

    @PostMapping("/api/v1/auth/register")
    public ResponseEntity<Long> register(@RequestBody RegisterRequest registerRequest) {
        return ResponseEntity.ok(this.userService.register(registerRequest));
    }

    @GetMapping("/api/v1/users/{id}")
    public ResponseEntity<UserDTO> getUser(@PathVariable Long id) {
         return ResponseEntity.ok(this.userService.getUserById(id));
    }

}
