package com.blockwin.protocol_api.web.controller;

import com.blockwin.protocol_api.model.dto.AuthenticationResponse;
import com.blockwin.protocol_api.model.dto.LoginRequest;
import com.blockwin.protocol_api.model.dto.RegisterRequest;
import com.blockwin.protocol_api.model.dto.UserDTO;
import com.blockwin.protocol_api.model.entity.UserEntity;
import com.blockwin.protocol_api.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/auth/login")
    public ResponseEntity<AuthenticationResponse> login(@RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(this.userService.login(loginRequest));
    }

    @PostMapping("/auth/register")
    public ResponseEntity<AuthenticationResponse> register(@RequestBody RegisterRequest registerRequest) {
        return ResponseEntity.ok(this.userService.register(registerRequest));
    }

    @PostMapping("/users/update")
    public ResponseEntity<AuthenticationResponse> updateUser(@RequestParam("id") Long id, @RequestBody RegisterRequest registerRequest) {
        return ResponseEntity.ok(this.userService.updateUser(id, registerRequest));
    }

    @GetMapping("/users")
    public ResponseEntity<UserDTO> getUser(@RequestParam("id") Long id) {
         return ResponseEntity.ok(this.userService.getUserById(id));
    }

}
