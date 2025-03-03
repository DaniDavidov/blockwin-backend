package com.blockwin.protocol_api.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
public class AdminController {

    @GetMapping("/api/v1/admin/configure")
    public ResponseEntity<String> configure() {
        return ResponseEntity.ok("Admin panel accessible!");
    }
}
